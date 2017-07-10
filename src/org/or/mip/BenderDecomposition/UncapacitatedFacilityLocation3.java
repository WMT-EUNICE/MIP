package org.or.mip.BenderDecomposition;

import com.dashoptimization.*;
import org.or.mip.Modelling.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by baohuaw on 7/10/17.
 */
public class UncapacitatedFacilityLocation3 extends UncapacitatedFacilityLocation {
    XPRB bcl;
    XPRBprob p;

    Map<String, XPRBprob> subModels = new HashMap<>();

    Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        UncapacitatedFacilityLocation3 location = new UncapacitatedFacilityLocation3();
        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/simpleExample.txt");
        long startTime = System.currentTimeMillis();
//        location.solveOriginalModel();
        location.solve();
        System.out.println("Solving the problem with " + (System.currentTimeMillis() - startTime) + " milli second");
    }

    static class myobj {
        XPRBprob prob;
        double tol;
    }


    protected void solve() {
        createVirtualFacility();

        bcl = new XPRB();                     /* Initialize BCL */
        p = bcl.newProb("Els");               /* Create a new problem in BCL */
        XPRS.init();                          /* Initialize Xpress-Optimizer */


        for (int i = 1; i <= numFacility; i++) {
            complicatingVarNames.add("y_" + i);
        }

        for (String locationVar : complicatingVarNames) {
            if (locationVar.equals("y_" + VIRTUAL_FACILITY))
                p.newVar(locationVar, XPRB.UI, 1, 1);
            else
                p.newVar(locationVar, XPRB.UI, 0, 1);
        }
        p.newVar("alpha", XPRB.PL, -10000, Double.MAX_VALUE);

//        Map<String, Double> objTerms = new LinkedHashMap<>();
        XPRBexpr obj = new XPRBexpr();
        for (int i = 1; i <= complicatingVarNames.size(); i++) {
            obj.addTerm(p.getVarByName(complicatingVarNames.get(i - 1)), openCosts.get(String.valueOf(i)));
        }
        obj.addTerm(p.getVarByName("alpha"), 1.0);
        p.setObj(obj);
//        masterSolver.setObj(objTerms);

        initSubModel();


        XPRSprob oprob;
        myobj mo;
        BendersCutMgrCallback cb;
        double feastol;
        int starttime, t;

        starttime = XPRB.getTime();

        oprob = p.getXPRSprob();                    /* Get Optimizer problem */

        oprob.setIntControl(XPRS.LPLOG, 0);
        oprob.setIntControl(XPRS.MIPLOG, 3);

        oprob.setIntControl(XPRS.CUTSTRATEGY, 0);   /* Disable automatic cuts */
        oprob.setIntControl(XPRS.PRESOLVE, 0);      /* Switch presolve off */
        oprob.setIntControl(XPRS.EXTRAROWS, 5000);  /* Reserve extra rows */

        feastol = oprob.getDblControl(XPRS.FEASTOL);  /* Get zero tolerance */
        feastol *= 10;

        mo = new myobj();
        mo.prob = p;
        mo.tol = feastol;
        p.setCutMode(1);

        cb = new BendersCutMgrCallback();
        oprob.addCutMgrListener(cb, mo);

        p.mipOptimize("");                          /* Solve the MIP */


    }

    void initSubModel() {
        for (int j = 1; j <= numCustomer; j++) {
            XPRBprob customer = bcl.newProb("Customer " + j);
            for (int i = 1; i <= numFacility; i++) {
                customer.newVar("x_" + i + "_" + j, XPRB.PL, 0, Double.MAX_VALUE);
            }

            for (String boundingVar : complicatingVarNames) {
                customer.newVar(boundingVar, XPRB.PL, 0, Double.MAX_VALUE);
            }

            XPRBexpr obj = new XPRBexpr();
            for (int i = 1; i <= numFacility; i++) {
                obj.addTerm(customer.getVarByName("x_" + i + "_" + j), servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
            }
            customer.setObj(obj);

            XPRBexpr ctr1 = new XPRBexpr();
            for (int i = 1; i <= numFacility; i++) {
                ctr1.addTerm(customer.getVarByName("x_" + i + "_" + j), 1.0);
            }
            customer.newCtr("Ctr 1", ctr1.eql(1));


            for (int i = 1; i <= numFacility; i++) {
                XPRBexpr temp = new XPRBexpr();
                temp.addTerm(customer.getVarByName("x_" + i + "_" + j), 1);
                temp.addTerm(customer.getVarByName("y_" + i), -1);
                customer.newCtr("Ctr 2 - " + i, temp.lEql(0));
            }


            for (String boundingVar : complicatingVarNames) {
                XPRBexpr bounding = new XPRBexpr();
                bounding.addTerm(customer.getVarByName(boundingVar), 1);
                customer.newCtr("Bounding with " + boundingVar, bounding.eql(0));
            }

            customer.setSense(XPRB.MINIM);
            subModels.put("Customer " + j, customer);

        }
    }

    class BendersCutMgrCallback implements XPRScutMgrListener {

        @Override
        public int XPRScutMgrEvent(XPRSprob xprSprob, Object o) {
            XPRBcut[] cuts = new XPRBcut[1];
            myobj mo;

            mo = (myobj) o;

            try {
      /* Get the solution values */
                mo.prob.beginCB(xprSprob);
                mo.prob.sync(XPRB.XPRS_SOL);

                if (!solveSubModel()) {
                    System.out.println("Error in solving sub model");
                }

                double sumOfBoundingMultipliedDual = 0;

//                Map<String, Double> cutTerms = new LinkedHashMap<>();
                XPRBexpr cut = new XPRBexpr();

                for (String masterVar : boundingVarSubDuals.keySet()) {
                    double masterVarCoeff = 0;
                    for (String subProblem : boundingVarSubDuals.get(masterVar).keySet()) {
                        masterVarCoeff += boundingVarSubDuals.get(masterVar).get(subProblem);
                        sumOfBoundingMultipliedDual += boundingVarSubDuals.get(masterVar).get(subProblem) * mo.prob.getVarByName(masterVar).getSol();
                    }
//                    cutTerms.put(masterVar, masterVarCoeff);
                    cut.addTerm(mo.prob.getVarByName(masterVar), masterVarCoeff);
                }
                cut.addTerm(mo.prob.getVarByName("alpha"), -1.0);

                double totalSubOptimum = 0;
                for (String subProblem : subModels.keySet()) {
                    totalSubOptimum += subModels.get(subProblem).getObjVal();

                }

                mo.prob.newCut(cut.lEql(-totalSubOptimum + sumOfBoundingMultipliedDual));
//                masterSolver.addConstraint();
//        masterSolver.addCut(masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
//                masterBendersCutId++;

                mo.prob.addCuts(cuts);
                mo.prob.endCB();
            } catch (XPRSprobException e) {
                System.out.println("Error  " + e.getCode() + ": " + e.getMessage());
            }
            return 0;
        }
    }

    boolean solveSubModel() {
        long time = System.currentTimeMillis();
        for (String subProblem : subModels.keySet()) {
            for (String boundingVar : complicatingVarNames) {
//                subSolvers.get(subProblem).setConstraintBound("Bounding with " +
//                                boundingVar, masterSolver.getVariableSol(boundingVar),
//                        masterSolver.getVariableSol(boundingVar));
                subModels.get(subProblem).getCtrByName("Bounding with " + boundingVar).setRange(p.getVarByName(boundingVar).getSol(),
                        p.getVarByName(boundingVar).getSol());
            }
            subModels.get(subProblem).lpOptimize();

//            subSolvers.get(subProblem).solveLP();

            for (String boundingVar : complicatingVarNames) {
                if (!boundingVarSubDuals.containsKey(boundingVar))
                    boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());

                boundingVarSubDuals.get(boundingVar).put(subProblem, subModels.get(subProblem).getCtrByName("Bounding with " + boundingVar).getDual());
            }

//            if (subSolvers.get(subProblem).getStatus() == ModelSolver.Status.OPTIMAL) {
////                System.out.println("Sub model objective value: " + subSolvers.get(subProblem).getOptimum());
//
//                for (String boundingVar : complicatingVarNames) {
//                    if (!boundingVarSubDuals.containsKey(boundingVar))
//                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());
//
//                    boundingVarSubDuals.get(boundingVar).put(subProblem, subSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
//                }
//            } else {
//                System.out.println("Sub model infeasible!");
//                return false;
//            }
//            else {
//                System.out.println("Sub model infeasible!");
//                for (String boundingVar : complicatingVarNames) {
//                    feasibleSubSolvers.get(subProblem).setConstraintBound("Bounding with " +
//                                    boundingVar, masterSolver.getVariableSol(boundingVar),
//                            masterSolver.getVariableSol(boundingVar));
//                }
//
//                feasibleSubSolvers.get(subProblem).solveLP();
//
//                if (feasibleSubSolvers.get(subProblem).getStatus() == ModelSolver.Status.OPTIMAL) {
//                    System.out.println("Sub model objective value: " + feasibleSubSolvers.get(subProblem).getOptimum());
//                } else {
//                    System.out.println("Bug exists when building the always feasible model");
//                }
//
//                for (String boundingVar : complicatingVarNames) {
//                    if (!boundingVarSubDuals.containsKey(boundingVar))
//                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());
//
//                    boundingVarSubDuals.get(boundingVar).put(subProblem, feasibleSubSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
//                }
//            }
        }
        System.out.println("Time for Solving all Sub : " + (System.currentTimeMillis() - time));
        return true;
    }

}
