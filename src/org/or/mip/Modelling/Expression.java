package org.or.mip.Modelling;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by baohuaw on 2017/6/30.
 */
public class Expression {
    public class Term {
        Variable var;
        double coef;

        public Term(Variable var, double coef) {
            this.var = var;
            this.coef = coef;
        }
    }
    String name;
    List<Term> terms = new LinkedList<>();
}
