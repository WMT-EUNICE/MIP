package org.or.mip.bb;

import com.dashoptimization.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by baohuaw on 6/23/17.
 */
public class XpressModeller {
    XPRB bcl = new XPRB();

    XPRBprob problem = bcl.newProb("Example");      /* Create a new problem in BCL */

    XPRBbasis basis;

    XPRBctr obj;

    double lb = 0;
    double ub = Double.MAX_VALUE;

    Map<String, XPRBvar> vars = new HashMap<>();



    public static void main(){


    }

    void buildModel(){

        XPRBvar x1 = problem.newVar(0, Double.MAX_VALUE);
        vars.put("x1", x1);
        XPRBvar x2 = problem.newVar(0, Double.MAX_VALUE);
        vars.put("x2", x2);

        XPRBexpr objExpr = new XPRBexpr();
        objExpr.add(x1.mul(-5));
        objExpr.add(x2.mul(-8));
        obj = problem.newCtr(objExpr);
        problem.setObj(obj);

        XPRBexpr constraint1 = new XPRBexpr();
        constraint1.add(x1);
        constraint1.add(x2);
        problem.newCtr(constraint1.lEql(6));

        XPRBexpr constraint2 = new XPRBexpr();
        constraint1.add(x1.mul(5));
        constraint1.add(x2.mul(9));
        problem.newCtr(constraint2.lEql(45));

        problem.setSense(XPRB.MINIM);
    }

    void solveRelaxedModel(){

        problem.lpOptimize("");             /* Solve the LP-problem */
        System.out.println("Objective: " + problem.getObjVal());  /* Get objective value */


        basis = problem.saveBasis();


    }

    void branching(){
        XPRBvar target = null;
        for(String varName : vars.keySet()){
            if(Math.abs(vars.get(varName).getSol() - (int)vars.get(varName).getSol()) >= 0.00001 ){
                target = vars.get(varName);
                break;
            }
        }

        if(target == null){
            System.out.println("Cannot find branching variable, terminate");
            return;
        }

        XPRBexpr leftBranching = new XPRBexpr();
    }
}
