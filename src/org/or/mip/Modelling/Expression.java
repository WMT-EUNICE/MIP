package org.or.mip.Modelling;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by baohuaw on 2017/6/30.
 */
public class Expression {
    String name;
    List<Term> terms = new LinkedList<>();

    public List<Term> getTerms() {
        return terms;
    }
}
