package org.or.mip.Modelling;

import java.util.Map;

/**
 * Created by baohuaw on 2017/6/30.
 */
public interface ModelSolver {
    public enum Status{
        OPTIMAL, ELSE
    }

    public enum Sense{
        MAX, MIN
    }

//    Model getModel();

//    void setModel(Model model);

    ModelSolverType getType();

    void solve();

    void addConstraint(Constraint constraint);

    void translateModel();

    void removeConstraint(Constraint constraint);

    double getDual(Constraint constraint);

    Expression getObj();

    void setObj(Expression obj);

    void setSense(Sense sense);

    Map<String, Variable> getVars();

    Map<String, Constraint> getConstraints();

    double getOptimum();

    Status getStatus();
}
