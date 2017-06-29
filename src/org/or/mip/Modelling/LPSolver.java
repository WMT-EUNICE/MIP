package org.or.mip.Modelling;

/**
 * Created by baohuaw on 2017/6/29.
 */
public interface LPSolver {
    Variable makeVar(String name, Variable.Type type, double lb, double ub);

    LPSolverType getType();
}
