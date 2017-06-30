package org.or.mip.Modelling;

/**
 * Created by baohuaw on 2017/6/30.
 */
public class Variable {
    String  name;
    VariableType type;
    double lb;
    double ub;

    public Variable(String name, VariableType type, double lb, double ub) {
        this.name = name;
        this.type = type;
        this.lb = lb;
        this.ub = ub;
    }

    double value;

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
