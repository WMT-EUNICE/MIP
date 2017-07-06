package org.or.mip.Modelling;

import com.dashoptimization.XPRB;
import com.dashoptimization.XPRBexpr;
import com.dashoptimization.XPRBprob;

/**
 * Created by baohuaw on 7/3/17.
 */
public class SolverUtils {

    public static XPRB bcl = new XPRB();

    public static void main(String[] args){
        originModel();
    }

    static void originModel(){
        XPRBprob problem = bcl.newProb("");

        problem.newVar("y1", XPRB.PL, 0, Double.MAX_VALUE);
        problem.newVar("y2", XPRB.PL, 0, Double.MAX_VALUE);
        problem.newVar("y3", XPRB.PL, 0, Double.MAX_VALUE);
        problem.newVar("x1", XPRB.PL, 0, Double.MAX_VALUE);
        problem.newVar("x2", XPRB.PL, 0, Double.MAX_VALUE);

        XPRBexpr obj = new XPRBexpr();
        obj.add(problem.getVarByName("y1").mul(-2));
        obj.add(problem.getVarByName("y2").mul(-1));
        obj.add(problem.getVarByName("y3").mul(1));
        obj.add(problem.getVarByName("x1").mul(3));
        obj.add(problem.getVarByName("x2").mul(-3));
        problem.setObj(obj);
        problem.setSense(XPRB.MINIM);

        XPRBexpr ctr1 = new XPRBexpr();
        ctr1.add(problem.getVarByName("y1").mul(1));
        ctr1.add(problem.getVarByName("x1").mul(1));
        ctr1.add(problem.getVarByName("x2").mul(1));
        problem.newCtr("ctr1", ctr1.lEql(3));

        XPRBexpr ctr2 = new XPRBexpr();
        ctr2.add(problem.getVarByName("y2").mul(2));
        ctr2.add(problem.getVarByName("x1").mul(3));
//        ctr1.add(problem.getVarByName("x2").mul(1));
        problem.newCtr("ctr2", ctr2.lEql(12));

        XPRBexpr ctr3 = new XPRBexpr();
        ctr3.add(problem.getVarByName("y3").mul(1));
        ctr3.add(problem.getVarByName("x2").mul(-7));
//        ctr1.add(problem.getVarByName("x2").mul(1));
        problem.newCtr("ctr3", ctr3.lEql(-16));

        XPRBexpr ctr4 = new XPRBexpr();
        ctr4.add(problem.getVarByName("x1").mul(-1));
        ctr4.add(problem.getVarByName("x2").mul(1));
//        ctr1.add(problem.getVarByName("x2").mul(1));
        problem.newCtr("ctr4", ctr4.lEql(2));

        problem.lpOptimise();
        if(problem.getLPStat() == XPRB.LP_OPTIMAL){
            System.out.println(problem.getObjVal());
        }else{
            System.out.println("Infeasible");
        }
    }
}
