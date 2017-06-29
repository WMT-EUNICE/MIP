package org.or.mip.BenderDecomposition;

import com.dashoptimization.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by baohuaw on 6/28/17.
 */
public class BenderDecompositionSolver {

    XPRB bcl = new XPRB();

//    XPRBprob problem = bcl.newProb("Example");      /* Create a new problem in BCL */

    XPRBprob master = bcl.newProb("Master");      /* Create a new problem in BCL */
    Map<String, XPRBprob> subs = new HashMap<>();
//    XPRBprob sub = bcl.newProb("Sub");      /* Create a new problem in BCL */


    XPRBexpr originalObj;
//    XPRBctr obj;
//    XPRBvar comlicatingVar;

    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;

//    Map<String, XPRBvar> vars = new HashMap<>();

    Map<String, XPRBvar> masterVars = new HashMap<>();
    Map<String, XPRBvar> subVars = new HashMap<>();

//    Map<String, XPRBctr> originalCons = new HashMap<>();


    public static void main(String[] args) {
        BenderDecompositionSolver solver = new BenderDecompositionSolver();
        solver.solve();
//        solver.init();
//        while (Math.abs(ub - lb))
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

//            master.lpOptimise();
//
//            lb = master.getObjVal();
        }
    }

    void init() {
        XPRBprob sub_y = bcl.newProb("Sub_y");
        subs.put("Sub_y", sub_y);
//        XPRBvar x = problem.newVar("x", XPRB.PL, 0, 16);
//        vars.put("x", x);
//        XPRBvar y = problem.newVar("y", XPRB.PL, 0, Double.MAX_VALUE);
//        vars.put("y", y);
//

//
//        XPRBexpr constraint1 = new XPRBexpr();
//        constraint1.add(vars.get("y").mul(1));
//        constraint1.add(vars.get("x").mul(-1));
//        originalCons.put("original cons 1", problem.newCtr("original cons 1", constraint1.lEql(5)));
//
//
//        XPRBexpr constraint2 = new XPRBexpr();
//        constraint2.add(vars.get("y").mul(1));
//        constraint2.add(vars.get("x").mul(-0.5));
//        originalCons.put("original cons 2", problem.newCtr("original cons 2", constraint2.lEql(7.5)));
//
//        XPRBexpr constraint3 = new XPRBexpr();
//        constraint3.add(vars.get("y").mul(1));
//        constraint3.add(vars.get("x").mul(0.5));
//        originalCons.put("original cons 3", problem.newCtr("original cons 3", constraint3.lEql(17.5)));
//
//        XPRBexpr constraint4 = new XPRBexpr();
//        constraint4.add(vars.get("y").mul(-1));
//        constraint4.add(vars.get("x").mul(1));
//        originalCons.put("original cons 4", problem.newCtr("original cons 4", constraint4.lEql(10)));
//
//        problem.setSense(XPRB.MINIM);
    }

    Map<String, Double> buildMasterModel(List<String> complicatingVarNames) {
//        XPRBvar complicatingVar = vars.get("x");
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

//        XPRBvar temp = master.getVarByName("alpha");

        master.lpOptimise();

        lb = master.getObjVal();

        Map<String, Double> complicatingVarBoundings = new HashMap<>();

        for (String complicatingVarName : complicatingVarNames) {
            complicatingVarBoundings.put(complicatingVarName, masterVars.get(complicatingVarName).getSol());
        }

        return complicatingVarBoundings;
    }

    Map<String, Double> buildSubModel(XPRBprob sub, Map<String, Double> complicatingVarBoundings) {

        XPRBvar y = sub.newVar("y", XPRB.PL, 0, Double.MAX_VALUE);
        subVars.put("y", y);


        for (String boundingVar : complicatingVarBoundings.keySet()) {
            XPRBvar var = sub.newVar(boundingVar, XPRB.PL, 0, Double.MAX_VALUE);
            subVars.put(boundingVar, var);
        }


        XPRBexpr objExpr = new XPRBexpr();
        objExpr.add(subVars.get("y").mul(-1));
        sub.setObj(objExpr);

        XPRBexpr constraint1 = new XPRBexpr();
        constraint1.add(subVars.get("y").mul(1));
        constraint1.add(subVars.get("x").mul(-1));
        sub.newCtr("original cons 1", constraint1.lEql(5));


        XPRBexpr constraint2 = new XPRBexpr();
        constraint2.add(subVars.get("y").mul(1));
        constraint2.add(subVars.get("x").mul(-0.5));
        sub.newCtr("original cons 2", constraint2.lEql(7.5));

        XPRBexpr constraint3 = new XPRBexpr();
        constraint3.add(subVars.get("y").mul(1));
        constraint3.add(subVars.get("x").mul(0.5));
        sub.newCtr("original cons 3", constraint3.lEql(17.5));

        XPRBexpr constraint4 = new XPRBexpr();
        constraint4.add(subVars.get("y").mul(-1));
        constraint4.add(subVars.get("x").mul(1));
        sub.newCtr("original cons 4", constraint4.lEql(10));


        Map<String, XPRBctr> boundingCtrs = new HashMap<>();
        for (String boundingVar : complicatingVarBoundings.keySet()) {
            XPRBexpr complicatingVarBoundingCons = new XPRBexpr();
            complicatingVarBoundingCons.add(subVars.get(boundingVar).mul(1));
            boundingCtrs.put(boundingVar, sub.newCtr("Bounding with " + boundingVar,
                    complicatingVarBoundingCons.eql(complicatingVarBoundings.get(boundingVar))));
        }

        sub.setSense(XPRB.MINIM);

        sub.lpOptimise();

//        ub = sub.getObjVal();
        System.out.println("Sub model objective value: " + sub.getObjVal());

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
