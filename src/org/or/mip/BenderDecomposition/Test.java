package org.or.mip.BenderDecomposition;

import com.dashoptimization.XPRB;
import com.dashoptimization.XPRBexpr;
import com.dashoptimization.XPRBprob;

/**
 * Created by baohuaw on 7/4/17.
 */
public class Test {
    static XPRB bcl = new XPRB();
    public static void main(String[] args){
        subModel2();
    }

    static void subModel1(){

        XPRBprob problem = bcl.newProb("");

        problem.newVar("y1", XPRB.PL, 0, Double.MAX_VALUE);
        problem.newVar("x1", XPRB.PL, 0, Double.MAX_VALUE);
        problem.newVar("x2", XPRB.PL, 0, Double.MAX_VALUE);

        XPRBexpr obj = new XPRBexpr();
        obj.add(problem.getVarByName("y1").mul(-2));
        problem.setObj(obj);
        problem.setSense(XPRB.MINIM);

        XPRBexpr ctr1 = new XPRBexpr();
        ctr1.add(problem.getVarByName("y1").mul(1));
        ctr1.add(problem.getVarByName("x1").mul(1));
        ctr1.add(problem.getVarByName("x2").mul(1));
        problem.newCtr("ctr1", ctr1.lEql(3));

        XPRBexpr ctr2 = new XPRBexpr();
//        ctr1.add(problem.getVarByName("y1").mul(1));
        ctr2.add(problem.getVarByName("x1").mul(1));
//        ctr1.add(problem.getVarByName("x2").mul(1));
        problem.newCtr("ctr2", ctr2.eql(0));

        XPRBexpr ctr3 = new XPRBexpr();
//        ctr1.add(problem.getVarByName("y1").mul(1));
        ctr3.add(problem.getVarByName("x2").mul(1));
//        ctr1.add(problem.getVarByName("x2").mul(1));
        problem.newCtr("ctr3", ctr3.eql(2));

        problem.lpOptimise();
        if(problem.getLPStat() == XPRB.LP_OPTIMAL){
            System.out.println(problem.getObjVal());
        }else{
            System.out.println("Infeasible");
        }
    }

    static void subModel2(){

        XPRBprob problem = bcl.newProb("");

        problem.newVar("y2", XPRB.PL, 0, Double.MAX_VALUE);
        problem.newVar("x1", XPRB.PL, 0, Double.MAX_VALUE);
        problem.newVar("x2", XPRB.PL, 0, Double.MAX_VALUE);

        XPRBexpr obj = new XPRBexpr();
        obj.add(problem.getVarByName("y2").mul(-1));
        problem.setObj(obj);
        problem.setSense(XPRB.MINIM);

        XPRBexpr ctr1 = new XPRBexpr();
        ctr1.add(problem.getVarByName("y2").mul(2));
        ctr1.add(problem.getVarByName("x1").mul(3));
//        ctr1.add(problem.getVarByName("x2").mul(1));
        problem.newCtr("ctr1", ctr1.lEql(12));

        XPRBexpr ctr2 = new XPRBexpr();
//        ctr1.add(problem.getVarByName("y1").mul(1));
        ctr2.add(problem.getVarByName("x1").mul(1));
//        ctr1.add(problem.getVarByName("x2").mul(1));
        problem.newCtr("ctr2", ctr2.eql(0));

        XPRBexpr ctr3 = new XPRBexpr();
//        ctr1.add(problem.getVarByName("y1").mul(1));
        ctr3.add(problem.getVarByName("x2").mul(1));
//        ctr1.add(problem.getVarByName("x2").mul(1));
        problem.newCtr("ctr3", ctr3.eql(2));

        problem.lpOptimise();

        if(problem.getLPStat() == XPRB.LP_OPTIMAL){
            System.out.println(problem.getObjVal());
        }else{
            System.out.println("Infeasible");
        }

    }
}
