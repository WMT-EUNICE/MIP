package org.or.mip.Modelling;

/**
 * Created by baohuaw on 2017/6/30.
 */
public interface ModelSolver {
    Model getModel();

    void setModel(Model model);

    ModelSolverType getType();

    void solve();
}
