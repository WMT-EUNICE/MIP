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

    XPRBprob problem = bcl.newProb("Example");      /* Create a new problem in BCL */

    XPRBprob master = bcl.newProb("Master");      /* Create a new problem in BCL */
    Map<String, XPRBprob> subs = new HashMap<>();
//    XPRBprob sub = bcl.newProb("Sub");      /* Create a new problem in BCL */


    XPRBexpr originalObj;
//    XPRBctr obj;
//    XPRBvar comlicatingVar;

    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;

    Map<String, XPRBvar> vars = new HashMap<>();

    Map<String, XPRBvar> masterVars = new HashMap<>();
    Map<String, XPRBvar> subVars = new HashMap<>();

    Map<String, XPRBctr> originalCons = new HashMap<>();


    public static void main(String[] args) {
        BenderDecompositionSolver solver = new BenderDecompositionSolver();
//        solver.init();
//        while (Math.abs(ub - lb))
    }

    void solve() {
        init();
        List<String> complicatingVarNames = new ArrayList<>();
        complicatingVarNames.add("x");

        Map<XPRBvar, Double> complicatingVarBoundings = buildMasterModel(complicatingVarNames);

        Map<XPRBvar, Double> boundingVarDuals = buildSubModel(subs.get("sub"), complicatingVarBoundings);

        addBendersCutToMaster(boundingVarDuals, subs.get("sub"), complicatingVarBoundings);

        master.lpOptimise();

        lb = master.getObjVal();

        while (Math.abs(ub - lb) >= 1) {
            for (String complicatingVarName : complicatingVarNames) {
                complicatingVarBoundings.put(masterVars.get(complicatingVarName), masterVars.get(complicatingVarName).getSol());
            }


            boundingVarDuals = buildSubModel(subs.get("sub"), complicatingVarBoundings);

            addBendersCutToMaster(boundingVarDuals, subs.get("sub"), complicatingVarBoundings);

            master.lpOptimise();

            lb = master.getObjVal();
        }
    }

    void init() {

        XPRBvar x = problem.newVar("x", XPRB.PL, 0, 16);
        vars.put("x", x);
        XPRBvar y = problem.newVar("y", XPRB.PL, 0, Double.MAX_VALUE);
        vars.put("y", y);

        originalObj = new XPRBexpr();
        originalObj.add(x.mul(-0.25));
        originalObj.add(y.mul(-1));
        problem.setObj(originalObj);

        XPRBexpr constraint1 = new XPRBexpr();
        constraint1.add(vars.get("y").mul(1));
        constraint1.add(vars.get("x").mul(-1));
        originalCons.put("original cons 1", problem.newCtr("original cons 1", constraint1.lEql(5)));


        XPRBexpr constraint2 = new XPRBexpr();
        constraint2.add(vars.get("y").mul(1));
        constraint2.add(vars.get("x").mul(-0.5));
        originalCons.put("original cons 2", problem.newCtr("original cons 2", constraint2.lEql(7.5)));

        XPRBexpr constraint3 = new XPRBexpr();
        constraint3.add(vars.get("y").mul(1));
        constraint3.add(vars.get("x").mul(0.5));
        originalCons.put("original cons 3", problem.newCtr("original cons 3", constraint3.lEql(17.5)));

        XPRBexpr constraint4 = new XPRBexpr();
        constraint4.add(vars.get("y").mul(-1));
        constraint4.add(vars.get("x").mul(1));
        originalCons.put("original cons 4", problem.newCtr("original cons 4", constraint4.lEql(10)));

        problem.setSense(XPRB.MINIM);
    }

    Map<XPRBvar, Double> buildMasterModel(List<String> complicatingVarNames) {
//        XPRBvar complicatingVar = vars.get("x");
        for (String complicatingVarName : complicatingVarNames) {
            masterVars.put(complicatingVarName, vars.get(complicatingVarName));
        }

        XPRBvar alpha = problem.newVar(XPRB.PL, -25, Double.MAX_VALUE);
        masterVars.put("alpha", alpha);


        XPRBexpr objExpr = new XPRBexpr();
        objExpr.add(masterVars.get("x").mul(-0.25));
        objExpr.add(masterVars.get("alpha").mul(1));
        master.setObj(objExpr);

        master.lpOptimise();

        lb = master.getObjVal();

        Map<XPRBvar, Double> complicatingVarBoundings = new HashMap<>();

        for (String complicatingVarName : complicatingVarNames) {
            complicatingVarBoundings.put(masterVars.get(complicatingVarName), masterVars.get(complicatingVarName).getSol());
        }

        return complicatingVarBoundings;
    }

    Map<XPRBvar, Double> buildSubModel(XPRBprob sub, Map<XPRBvar, Double> complicatingVarBoundings) {
        subVars.put("y", vars.get("y"));

        for (XPRBvar boundingVar : complicatingVarBoundings.keySet()) {
            subVars.put(boundingVar.getName(), boundingVar);
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


        Map<XPRBvar, XPRBctr> boundingCtrs = new HashMap<>();
        for (XPRBvar boundingVar : complicatingVarBoundings.keySet()) {
            XPRBexpr complicatingVarBoundingCons = new XPRBexpr();
            complicatingVarBoundingCons.add(boundingVar.mul(1));
            boundingCtrs.put(boundingVar, sub.newCtr("Bounding with " + boundingVar.getName(),
                    complicatingVarBoundingCons.eql(boundingVar)));
        }

        sub.setSense(XPRB.MINIM);

        sub.lpOptimise();

//        ub = sub.getObjVal();

        Map<XPRBvar, Double> boundingDuals = new HashMap<>();
        for (XPRBvar boundingVar : boundingCtrs.keySet()) {
            boundingDuals.put(boundingVar, boundingCtrs.get(boundingVar).getDual());
        }

        ub = originalObj.getSol();

        return boundingDuals;
    }

    void addBendersCutToMaster(Map<XPRBvar, Double> masterVarDuals, XPRBprob sub, Map<XPRBvar, Double> masterVarBoundings) {
        XPRBexpr cutExpr = new XPRBexpr();

        double sumOfBoundingMultipliedDual = 0;
        for (XPRBvar masterVar : masterVarDuals.keySet()) {
            cutExpr.add(masterVar.mul(masterVarDuals.get(masterVar)));
            sumOfBoundingMultipliedDual += masterVarDuals.get(masterVar) * masterVarBoundings.get(masterVar);
        }
        cutExpr.add(masterVars.get("alpha").mul(-1));


        master.newCtr("Benders Cut", cutExpr.lEql(-sub.getObjVal() + sumOfBoundingMultipliedDual));


    }

}
