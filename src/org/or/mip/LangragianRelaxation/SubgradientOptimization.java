package org.or.mip.LangragianRelaxation;

import com.dashoptimization.XPRB;
import com.dashoptimization.XPRBexpr;
import com.dashoptimization.XPRBprob;
import com.dashoptimization.XPRBvar;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by baohuaw on 6/29/17.
 */
public class SubgradientOptimization {
    XPRB bcl = new XPRB();

    XPRBprob original = bcl.newProb("Original");      /* Create a new problem in BCL */

    XPRBprob relaxation = bcl.newProb("Relaxation");      /* Create a new problem in BCL */

    Map<String, XPRBvar> originalVars = new HashMap<>();
    Map<String, XPRBvar> relaxationVars = new HashMap<>();
//    Map<String, XPRBvar> multipliers = new HashMap<>();

//    double ub = 0;
    double currentBest = 0;

    double theta = 2;   //param for step size

    double lamda = 0;  //lagragian multiplier

    double t = 1;  //scalar

    public static void main(String[] args) {
        SubgradientOptimization solver = new SubgradientOptimization();
        solver.solveOriginalModel();
        solver.subgradientOptimization();
    }

    void solveOriginalModel() {
        originalVars.put("x1", original.newVar("x1", XPRB.BV, 0, 1));
        originalVars.put("x2", original.newVar("x2", XPRB.BV, 0, 1));
        originalVars.put("x3", original.newVar("x3", XPRB.BV, 0, 1));
        originalVars.put("x4", original.newVar("x4", XPRB.BV, 0, 1));

        XPRBexpr originalObj = new XPRBexpr();
        originalObj.add(originalVars.get("x1").mul(8));
        originalObj.add(originalVars.get("x2").mul(9));
        originalObj.add(originalVars.get("x3").mul(5));
        originalObj.add(originalVars.get("x4").mul(4));
        original.setObj(originalObj);

        XPRBexpr cons1 = new XPRBexpr();
        cons1.add(originalVars.get("x1").mul(16));
        cons1.add(originalVars.get("x2").mul(20));
        cons1.add(originalVars.get("x3").mul(12));
        cons1.add(originalVars.get("x4").mul(10));
        original.newCtr("Cons 1", cons1.lEql(42));

//        XPRBexpr cons2 = new XPRBexpr();
//        cons2.add(originalVars.get("x1").mul(1));
//        cons2.add(originalVars.get("x2").mul(1));
//        original.newCtr("Cons 2", cons2.lEql(1));
//
//        XPRBexpr cons3 = new XPRBexpr();
//        cons3.add(originalVars.get("x3").mul(1));
//        cons3.add(originalVars.get("x4").mul(1));
//        original.newCtr("Cons 3", cons3.lEql(1));

        original.setSense(XPRB.MAXIM);

        original.mipOptimise();

        System.out.println("Original Obj " + original.getObjVal());

        System.out.println("x1 = " + originalVars.get("x1").getSol());
        System.out.println("x2 = " + originalVars.get("x2").getSol());
        System.out.println("x3 = " + originalVars.get("x3").getSol());
        System.out.println("x4 = " + originalVars.get("x4").getSol());
    }

    void subgradientOptimization() {

        relaxationVars.put("x1", relaxation.newVar("x1", XPRB.BV, 0, 1));
        relaxationVars.put("x2", relaxation.newVar("x2", XPRB.BV, 0, 1));
        relaxationVars.put("x3", relaxation.newVar("x3", XPRB.BV, 0, 1));
        relaxationVars.put("x4", relaxation.newVar("x4", XPRB.BV, 0, 1));

        XPRBexpr relaxObj = new XPRBexpr();
        relaxObj.add(relaxationVars.get("x1").mul(8 - 16 * lamda));
        relaxObj.add(relaxationVars.get("x2").mul(9 - 20 * lamda));
        relaxObj.add(relaxationVars.get("x3").mul(5 - 12 * lamda));
        relaxObj.add(relaxationVars.get("x4").mul(4 - 10 * lamda));
        relaxation.setObj(relaxObj);
        relaxation.setSense(XPRB.MAXIM);

//        XPRBexpr cons2 = new XPRBexpr();
//        cons2.add(relaxationVars.get("x1").mul(1));
//        cons2.add(relaxationVars.get("x2").mul(1));
//        relaxation.newCtr("Cons 2", cons2.lEql(1));
//
//        XPRBexpr cons3 = new XPRBexpr();
//        cons3.add(relaxationVars.get("x3").mul(1));
//        cons3.add(relaxationVars.get("x4").mul(1));
//        relaxation.newCtr("Cons 3", cons3.lEql(1));

        relaxation.mipOptimise();

        double obj = relaxation.getObjVal() + 42 * lamda;
        System.out.println("Relax Obj " + obj);
        System.out.println("x1 = " + originalVars.get("x1").getSol());
        System.out.println("x2 = " + originalVars.get("x2").getSol());
        System.out.println("x3 = " + originalVars.get("x3").getSol());
        System.out.println("x4 = " + originalVars.get("x4").getSol());

//        double subGradient =
        int step = 0;

        while (step < 50) {
            double subgradient = 42 - relaxationVars.get("x1").getSol() * 16 - relaxationVars.get("x2").getSol() * 20 -
                    relaxationVars.get("x3").getSol() * 12 - relaxationVars.get("x4").getSol() * 10;
            t = theta * (relaxation.getObjVal() - currentBest) / (subgradient * subgradient);

            System.out.println("Step " + step + "  Lamda = " + lamda);
            lamda = Math.max(0, (lamda - t * subgradient));

//            t = t / 3;


            if (subgradient >= 0) {
                currentBest = relaxation.getObjVal();
            }

            relaxObj.reset();
            relaxObj.add(relaxationVars.get("x1").mul(8 - 16 * lamda));
            relaxObj.add(relaxationVars.get("x2").mul(9 - 20 * lamda));
            relaxObj.add(relaxationVars.get("x3").mul(5 - 12 * lamda));
            relaxObj.add(relaxationVars.get("x4").mul(4 - 10 * lamda));
            relaxation.setObj(relaxObj);
            relaxation.setSense(XPRB.MAXIM);
            relaxation.mipOptimise();

            obj = relaxation.getObjVal() + 42 * lamda;
            System.out.println("Relax Obj " + obj);
            System.out.println("x1 = " + originalVars.get("x1").getSol());
            System.out.println("x2 = " + originalVars.get("x2").getSol());
            System.out.println("x3 = " + originalVars.get("x3").getSol());
            System.out.println("x4 = " + originalVars.get("x4").getSol());


            step++;
        }


    }
}
