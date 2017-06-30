package org.or.mip.Modelling;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by baohuaw on 2017/6/30.
 */
public class Model {
    public enum Sense{
        MAX, MIN
    }
    String name;
    Expression obj;
    List<Constraint> constraints = new LinkedList<>();
    List<Variable> vars = new LinkedList<>();

    Sense sense;
    double optimum;
}
