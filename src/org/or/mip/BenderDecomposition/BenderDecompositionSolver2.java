package org.or.mip.BenderDecomposition;

import com.dashoptimization.*;
import org.or.mip.Modelling.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by baohuaw on 6/28/17.
 *
 * Chapter 3 Decompistion Techniques in Integer Programming
 */
public class BenderDecompositionSolver2 {
    ModelSolver subSolver = new XpressSolver();
    ModelSolver masterSolver = new XpressSolver();

    Model master = new Model();
    Map<String, Model> subs = new HashMap<>();


//    XPRB bcl = new XPRB();

//    XPRBprob master = bcl.newProb("Master");      /* Create a new problem in BCL */
//    Map<String, XPRBprob> subs = new HashMap<>();

    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;

    Map<String, Variable> masterVars = new HashMap<>();
    Map<String, Variable> subVars = new HashMap<>();

    public static void main(String[] args) {
        BenderDecompositionSolver2 solver = new BenderDecompositionSolver2();
        solver.solve();
    }

    void solve() {
        init();
        List<String> complicatingVarNames = new ArrayList<>();
        complicatingVarNames.add("x");

        Map<String, Double> complicatingVarBoundings = buildMasterModel(complicatingVarNames);

        Map<String, Double> boundingVarDuals = buildSubModel(subs.get("Sub_y"), complicatingVarBoundings);

        addBendersCutToMaster(boundingVarDuals, subs.get("Sub_y"), complicatingVarBoundings);



        while (Math.abs(ub - lb) >= 1) {

            master.lpOptimise();

            lb = master.getObjVal();

            for (String complicatingVarName : complicatingVarNames) {
                complicatingVarBoundings.put(complicatingVarName, masterVars.get(complicatingVarName).getSol());

                XPRBctr boundingCtr = subs.get("Sub_y").getCtrByName("Bounding with " + complicatingVarName);
                boundingCtr.setRange(masterVars.get(complicatingVarName).getSol(), masterVars.get(complicatingVarName).getSol());
            }


            subs.get("Sub_y").lpOptimise();

            System.out.println("Sub model objective value: " + subs.get("Sub_y").getObjVal());

//            Map<String, Double> boundingDuals = new HashMap<>();

            for (String complicatingVarName : complicatingVarNames) {
                XPRBctr boundingCtr = subs.get("Sub_y").getCtrByName("Bounding with " + complicatingVarName);
                boundingVarDuals.put(complicatingVarName, boundingCtr.getDual());
            }

            ub = -subVars.get("x").getSol() * 0.25 - subVars.get("y").getSol();


            addBendersCutToMaster(boundingVarDuals, subs.get("Sub_y"), complicatingVarBoundings);
        }
    }

    void init() {
//        XPRBprob sub_y = bcl.newProb("Sub_y");
        Model sub_y = new Model();
        subs.put("Sub_y", sub_y);

        subSolver.setModel(sub_y);

    }

    Map<String, Double> buildMasterModel(List<String> complicatingVarNames) {
        master.newVar("x", XPRB.PL, 0, 16);
        for (String complicatingVarName : complicatingVarNames) {
            masterVars.put(complicatingVarName, master.getVarByName("x"));
        }

        XPRBvar alpha = master.newVar("alpha", XPRB.PL, -25, Double.MAX_VALUE);
        masterVars.put("alpha", alpha);


        XPRBexpr objExpr = new XPRBexpr();
        objExpr.add(masterVars.get("x").mul(-0.25));
        objExpr.add(masterVars.get("alpha").mul(1));
        master.setObj(objExpr);

        master.setSense(XPRB.MINIM);

        master.lpOptimise();

        lb = master.getObjVal();

        Map<String, Double> complicatingVarBoundings = new HashMap<>();

        for (String complicatingVarName : complicatingVarNames) {
            complicatingVarBoundings.put(complicatingVarName, masterVars.get(complicatingVarName).getSol());
        }

        return complicatingVarBoundings;
    }

    Map<String, Double> buildSubModel(Model sub, Map<String, Double> complicatingVarBoundings) {

//        XPRBvar y = sub.newVar("y", XPRB.PL, 0, Double.MAX_VALUE);
        Variable y = new Variable("y", VariableType.REAL, 0 , Double.MAX_VALUE);
        subVars.put("y", y);


        for (String boundingVar : complicatingVarBoundings.keySet()) {
            Variable var = new Variable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
            subVars.put(boundingVar, var);
        }

        Expression objExpression = new Expression();
        objExpression.getTerms().add(new Term(subVars.get("y"), -1));
        sub.setObj(objExpression);

        Constraint constraint1 = new Constraint(ConstraintType.LEQL,5);
        constraint1.getTerms().add(new Term(subVars.get("y"), 1));
        constraint1.getTerms().add(new Term(subVars.get("x"), -1));
        sub.getConstraints().add(constraint1);

        Constraint constraint2 = new Constraint(ConstraintType.LEQL,7.5);
        constraint2.getTerms().add(new Term(subVars.get("y"), 1));
        constraint2.getTerms().add(new Term(subVars.get("x"), -0.5));
        sub.getConstraints().add(constraint2);

        Constraint constraint3 = new Constraint(ConstraintType.LEQL,17.5);
        constraint3.getTerms().add(new Term(subVars.get("y"), 1));
        constraint3.getTerms().add(new Term(subVars.get("x"), 0.5));
        sub.getConstraints().add(constraint3);

        Constraint constraint4 = new Constraint(ConstraintType.LEQL,10);
        constraint4.getTerms().add(new Term(subVars.get("y"), -1));
        constraint4.getTerms().add(new Term(subVars.get("x"), 1));
        sub.getConstraints().add(constraint4);

        Map<String, Constraint> boundingCtrs = new HashMap<>();
        for (String boundingVar : complicatingVarBoundings.keySet()) {
//            XPRBexpr complicatingVarBoundingCons = new XPRBexpr();
            Constraint complicatingVarBoundingCons = new Constraint(ConstraintType.EQL, complicatingVarBoundings.get(boundingVar));
            complicatingVarBoundingCons.getTerms().add(new Term(subVars.get(boundingVar), 1));
            boundingCtrs.put("Bounding with " + boundingVar, complicatingVarBoundingCons);
//            boundingCtrs.put(boundingVar, sub.newCtr("Bounding with " + boundingVar,
//                    complicatingVarBoundingCons.eql(complicatingVarBoundings.get(boundingVar))));
        }

        sub.setSense(Model.Sense.MIN);
        subSolver.solve();

//        ub = sub.getObjVal();
        System.out.println("Sub model objective value: " + sub.getOptimum());

        Map<String, Double> boundingDuals = new HashMap<>();
        for (String boundingVar : boundingCtrs.keySet()) {
            boundingDuals.put(boundingVar, boundingCtrs.get(boundingVar).getDual());
        }


        ub = -subVars.get("x").getSol() * 0.25 - subVars.get("y").getSol();

        return boundingDuals;
    }

    void addBendersCutToMaster(Map<String, Double> masterVarDuals, XPRBprob sub, Map<String, Double> masterVarBoundings) {
        XPRBexpr cutExpr = new XPRBexpr();

        double sumOfBoundingMultipliedDual = 0;


        for (String masterVar : masterVarDuals.keySet()) {
            cutExpr.add(masterVars.get(masterVar).mul(masterVarDuals.get(masterVar)));
            sumOfBoundingMultipliedDual += masterVarDuals.get(masterVar) * masterVarBoundings.get(masterVar);
        }
        cutExpr.add(masterVars.get("alpha").mul(-1));

        master.newCtr("Benders Cut", cutExpr.lEql(-sub.getObjVal() + sumOfBoundingMultipliedDual));
    }

}
