package org.or.mip.Modelling;

import java.util.List;
import java.util.Map;

/**
 * Created by baohuaw on 2017/7/3.
 */
public interface Model {
    public enum Status {
        OPTIMAL, ELSE
    }

    public enum Sense{
        MAX, MIN
    }



    void addVariable(String name, VariableType type, double lb, double ub);

    void addConstraint(String name, Map<String, Double> terms, ConstraintType type, double lb, double ub);

    void addCut(int id, Map<String, Double> terms, ConstraintType type, double lb, double ub);

    void solveLP();

    void solveByBranchAndBound(List<String> varNames);

    void setVariableType(String varName, VariableType type);

    void solveMIP();

    void addInitialSolution(Map<String,Double> varValues);

    void removeConstraint(String ctrName);

    double getVariableSol(String varName);

    double getOptimum();

    double getDual(String ctrName);

    String getName();

    Status getStatus();

    void setConstraintBound(String ctrName, double lb, double ub);

    void setVariableBound(String varName, double lb, double ub);

    void setSense(Sense sense);

    void setObj(Map<String, Double> terms);

    double getSlack(String ctrName);

    boolean hasConstraint(String ctrName);

    void printConstraint(String ctrName);
}
