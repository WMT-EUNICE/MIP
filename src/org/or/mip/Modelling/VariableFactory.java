package org.or.mip.Modelling;

/**
 * Created by baohuaw on 2017/6/29.
 */
public class VariableFactory {
    public static Variable create(LPSolver lpSolver, String name, Variable.Type type, double lb, double ub) {
        if (lpSolver.getType() == LPSolverType.XPRESS) {
            return lpSolver.makeVar(name, type, lb, ub);
        }
        return null;
    }
}
