package org.or.mip.Modelling;

/**
 * Created by baohuaw on 2017/6/29.
 */
public interface Variable {
    public enum Type {
        REAL, BINARY, INTEGER
    }

    String getName();

    double getLowerBound();

    double getUpperBound();


}
