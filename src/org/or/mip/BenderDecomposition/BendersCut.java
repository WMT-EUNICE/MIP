package org.or.mip.BenderDecomposition;

import org.or.mip.Modelling.ConstraintType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by baohuaw on 7/13/17.
 */
public class BendersCut {
    String name;
    Map<String, Double> terms = new LinkedHashMap<>();
    ConstraintType type;
    double lb;
    double ub;

    public String getName() {
        return name;
    }

    public Map<String, Double> getTerms() {
        return terms;
    }

    public ConstraintType getType() {
        return type;
    }

    public double getLb() {
        return lb;
    }

    public double getUb() {
        return ub;
    }

    public BendersCut(String name, Map<String, Double> terms, ConstraintType type, double lb, double ub) {
        this.name = name;
        this.terms = terms;
        this.type = type;
        this.lb = lb;
        this.ub = ub;
    }
}
