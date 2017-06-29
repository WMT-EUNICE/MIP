package org.or.mip.Modelling;

import com.dashoptimization.XPRBvar;

/**
 * Created by baohuaw on 2017/6/29.
 */
public class XpressVariable implements Variable {
    XPRBvar element;

//    String name;
//    double lb;
//    double ub;
    public XpressVariable(LPSolver solver, String name, Variable.Type type, double lb, double ub) {
        element =  (XPRBvar) VariableFactory.create(solver, name, type, lb, ub);
    }

    @Override
    public String getName() {
        return element.getName();
    }

    @Override
    public double getLowerBound() {
        return element.getLB();
    }

    @Override
    public double getUpperBound() {
        return element.getUB();
    }
}
