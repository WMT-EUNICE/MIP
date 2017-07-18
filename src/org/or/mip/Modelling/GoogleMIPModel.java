package org.or.mip.Modelling;

/**
 * Created by baohuaw on 7/18/17.
 */
public class GoogleMIPModel extends GoogleLPModel {
    public GoogleMIPModel(String name) {
        super(name);
    }

    @Override
    public void solveMIP() {
        solver.solve();
//        System.out.println("This is a LP solver");
//        status = solver.solve();
    }
}
