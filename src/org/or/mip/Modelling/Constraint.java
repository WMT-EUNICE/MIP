package org.or.mip.Modelling;

/**
 * Created by baohuaw on 2017/6/30.
 */
public class Constraint extends Expression{
    ConstraintType type;
    double bound;

    public Constraint(ConstraintType type, double bound) {
        this.type = type;
        this.bound = bound;
    }

    public ConstraintType getType() {
        return type;
    }

    public void setType(ConstraintType type) {
        this.type = type;
    }

    public double getBound() {
        return bound;
    }

    public void setBound(double bound) {
        this.bound = bound;
    }


}
