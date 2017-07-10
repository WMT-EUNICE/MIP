package org.or.mip.BenderDecomposition;

import org.or.mip.Modelling.Model;

import java.io.IOException;
import java.util.*;

/**
 * Created by baohuaw on 7/10/17.
 */
public class UFLHeuristic extends UncapacitatedFacilityLocation4 {
    public static void main(String[] args) throws IOException {
        UFLHeuristic location = new UFLHeuristic();
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/GalvaoRaggi/200/200.9");
        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/KoerkelGhosh-sym/500/a/gs500a-1");
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/simpleExample.txt");
        long startTime = System.currentTimeMillis();
//        location.solveOriginalModel();
        location.solve();
        System.out.println("Solving the problem with " + (System.currentTimeMillis() - startTime) + " milli second");
    }

    protected void solve() {
//        createVirtualFacility();

        initMaster();
        initSubModel();

        solveMasterModel();
        Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();
        if (!solveSubModel(boundingVarSubDuals)) {
            System.out.println("Terminate due to sub problem infeasibility!");
            return;
        }

//        checkNearestConnection();


        updateUB();
        addBendersCutToMaster(boundingVarSubDuals);

        int step = 0;


        while (Math.abs(ub - lb) >= 1) {

            if (step % 1 == 0) {
                System.out.println(step + ",  " + lb + ",  " + ub + ",  " + (ub - lb));
//                System.out.println("UB = " + ub);
//                System.out.println("LB = " + lb);
            }
            solveMasterModel();
            if (!solveSubModel(boundingVarSubDuals)) {
                System.out.println("Terminate due to sub problem infeasibility!");
                return;
            }
            updateUB();

            addBendersCutToMaster(boundingVarSubDuals);
            step++;
        }

        System.out.println("Upper bound = " + ub);
        System.out.println("Lower bound = " + lb);

        double totalCost = 0;
        for (int i = 1; i <= numFacility; i++) {
            if (Math.abs(masterSolver.getVariableSol("y_" + i) - 1) <= 0.0001) {
                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(String.valueOf(i)));
                totalCost += openCosts.get(String.valueOf(i));
            }
        }

        for (int j = 1; j <= numCustomer; j++) {
            for (int i = 1; i < numFacility; i++) {
                if (Math.abs(subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) - 1) < 0.0001) {
                    System.out.println("Customer " + j + " is served by Facility " + i + " with cost of " + servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
                    totalCost += servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                }
            }
        }

        System.out.println("Total cost " + (totalCost));
        System.out.println("Benders Cut " + masterBendersCutId);
    }

    void localSearch(){
        //find the highest connection cost customer
        double maxConnectionCost = 0;
        String maxConnectionCostCustomer = null;
        for (int j = 1; j <= numCustomer; j++) {
            Model sub = subSolvers.get("Customer " + j);
            for (int i = 1; i <= numFacility; i++) {
                if (Math.abs(sub.getVariableSol("x_" + i + "_" + j) - 1) <= 0.0001) {
                    if(servingCosts.get(String.valueOf(i)).get(String.valueOf(j)) > maxConnectionCost){
                        maxConnectionCost = servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                        maxConnectionCostCustomer = String.valueOf(j);
                    }
                }
            }
        }



    }

    void checkNearestConnection() {
        for (int j = 1; j <= numCustomer; j++) {
            Model sub = subSolvers.get("Customer " + j);
            String connectionFacility = null;
            for (int i = 1; i <= numFacility; i++) {
                if (Math.abs(sub.getVariableSol("x_" + i + "_" + j) - 1) <= 0.0001) {
                    connectionFacility = String.valueOf(i);
                }
            }

            double minDistance = Double.MAX_VALUE;
            String minDistanceFac = null;
            for(int i = 1;i <= numFacility;i++){
                if(servingCosts.get(String.valueOf(i)).get(String.valueOf(j)) < minDistance){
                    minDistance = servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                    minDistanceFac = String.valueOf(i);
                }
            }

            if(!connectionFacility.equals(minDistanceFac)){
                System.out.println("Customer " + j + " is not connected to the nearest facility ");
            }
        }
//        Map<String, List<String>> facilityCustomerMapping = new HashMap<>();
//        for(int i = 1;i < numCustomer;i++){
////            int numConnection = 0;
//            List<String> connections = new LinkedList<>();
//            for( int j = 1;j < numCustomer;j++){
//                Model sub = subSolvers.get("Customer " + j);
//                if(Math.abs(sub.getVariableSol("x_" + i + "_" + j) - 1) <= 0.0001){
////                    numConnection++;
//                    connections.add(String.valueOf(j));
//                }
//            }
//            facilityCustomerMapping.put(String.valueOf(i), connections);
//        }

//        String target = null;
//        int minConnection = Integer.MAX_VALUE;
//        for(String facility : facilityCustomerMapping.keySet()){
//            if(facilityCustomerMapping.get(facility) < minConnection){
//                minConnection = facilityCustomerMapping.get(facility);
//                target = facility;
//            }
//        }


    }

}
