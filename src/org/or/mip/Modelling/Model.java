package org.or.mip.Modelling;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by baohuaw on 2017/6/30.
 */
public class Model {
    public enum Status{
        OPTIMAL, ELSE
    }

    public enum Sense{
        MAX, MIN
    }
    String name;
    Expression obj;
    List<Constraint> constraints = new LinkedList<>();
    List<Variable> vars = new LinkedList<>();

    Status status;

    Sense sense;
    double optimum;

    public Expression getObj() {
        return obj;
    }

    public void setObj(Expression obj) {
        this.obj = obj;
    }

    public List<Variable> getVars() {
        return vars;
    }

    public void setSense(Sense sense) {
        this.sense = sense;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    public double getOptimum() {
        return optimum;
    }

    public Status getStatus() {
        return status;
    }
}
