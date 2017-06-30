package org.or.mip.Modelling;

/**
 * Created by baohuaw on 2017/6/30.
 */
public class Term {
    Variable var;
    double coef;

    public Term(Variable var, double coef) {
        this.var = var;
        this.coef = coef;
    }
}
