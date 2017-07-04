package org.or.mip.Modelling;

import java.util.Map;

/**
 * Created by baohuaw on 2017/6/30.
 */
public interface ModelSolver {
    enum Status {
        OPTIMAL, ELSE
    }

    enum Sense {
        MAX, MIN
    }

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

    void setConstraintBound(Constraint constraint, double bound);
}
