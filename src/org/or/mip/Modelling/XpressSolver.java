package org.or.mip.Modelling;

import com.dashoptimization.XPRBprob;

/**
 * Created by baohuaw on 2017/6/29.
 */
public class XpressSolver implements LPSolver {
    XPRBprob prob;

    @Override
    public XpressVariable makeVar(String name, Variable.Type type, double lb, double ub) {
        if(type == Variable.Type.BINARY){
//            return prob.newVar(name, XPRB.BV, lb, ub);
        }else if(type == Variable.Type.REAL){
//            return prob.newVar(name, XPRB.BV, lb, ub);
        }else if(type == Variable.Type.INTEGER){

        }
        return null;
    }

    @Override
    public LPSolverType getType() {
        return null;
    }
}
