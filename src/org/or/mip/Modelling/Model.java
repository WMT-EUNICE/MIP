package org.or.mip.Modelling;

import java.util.Map;

/**
 * Created by baohuaw on 2017/7/3.
 */
public interface Model {
    void addVariable(String name, VariableType type, double lb, double ub);

    void addConstraint(String name, Map<String, Double> terms, ConstraintType type, double lb, double ub);

    void solveLP();

    void solveMIP();

    void removeConstraint(String ctrName);

    double getVariableSol(String varName);

    double getOptimum();

    double getDual(String ctrName);

    String getName();

    ModelSolver.Status getStatus();

    void setConstraintBound(String ctrName, double lb, double ub);

    void setSense(ModelSolver.Sense sense);

    void setObj(Map<String, Double> terms);
}
