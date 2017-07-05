package org.or.mip.BenderDecomposition;

import org.or.mip.Modelling.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by baohuaw on 7/5/17.
 */
public class UncapacitatedFacilityLocation {
    double numFacility;
    double numCustomer;
    Map<String, Model> subSolvers = new HashMap<>();
    Model masterSolver = new XpressModel("Master");
    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;
    List<String> complicatingVarNames = new ArrayList<>();

    Map<String, Double> openCosts = new LinkedHashMap<>();
    //facility->customer->cost
    Map<String, Map<String, Double>> servingCosts = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        UncapacitatedFacilityLocation location = new UncapacitatedFacilityLocation();
        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/simpleExample.txt");
        location.solve();
    }

    void readProblem(String fileName) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(fileName));

        String line;
        int lineId = 0;


        while ((line = in.readLine()) != null) {
            System.out.println(line);
            if (lineId == 0) {
                String[] elements = line.split(" ");
                numFacility = Double.valueOf(elements[0]);
                numCustomer = Double.valueOf(elements[1]);
                for (int i = 1; i <= numFacility; i++) {
                    openCosts.put(String.valueOf(i), 0.0);
                    if (!servingCosts.containsKey(String.valueOf(i))) {
                        servingCosts.put(String.valueOf(i), new LinkedHashMap<>());
                    }
                    for (int j = 1; j <= numCustomer; j++) {
                        servingCosts.get(String.valueOf(i)).put(String.valueOf(j), 0.0);
                    }
                }
            } else {
                String[] elements = line.split(" ");
                String facilityId = elements[0];
                double openCost = Double.valueOf(elements[1]);
                openCosts.put(facilityId, openCost);
                for (int j = 1; j <= elements.length - 2; j++) {
                    servingCosts.get(facilityId).put(String.valueOf(j), Double.valueOf(elements[j - 1 + 2]));
                }
            }
            lineId++;
        }
        in.close();
    }

    void initMaster() {
        for (int i = 1; i <= numFacility; i++) {
            complicatingVarNames.add("y" + i);
        }

        //definition of master model
        for (String locationVar : complicatingVarNames) {
            masterSolver.addVariable(locationVar, VariableType.BINARY, 0, 1);
        }
        masterSolver.addVariable("alpha", VariableType.REAL, -100, Double.MAX_VALUE);

        Map<String, Double> objTerms = new LinkedHashMap<>();


        for (int i = 1; i <= complicatingVarNames.size(); i++) {
            objTerms.put(complicatingVarNames.get(i - 1), openCosts.get(String.valueOf(i)));
        }
        objTerms.put("alpha", 1.0);
        masterSolver.setObj(objTerms);

//        Map<String, Double> ctr1Terms = new LinkedHashMap<>();
//        ctr1Terms.put("x1", -1.0);
//        ctr1Terms.put("x2", 1.0);
//        masterSolver.addConstraint("Master Ctr 1", ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 2);
        masterSolver.setSense(ModelSolver.Sense.MIN);
    }

    void initSubModel(){
        for(int j = 1;j <= numCustomer;j++){
            Model customer = new XpressModel("Customer " + j);
            for(int i = 1;i <= numFacility;i++){
                customer.addVariable("x" + i + "" + j, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            for (String boundingVar : complicatingVarNames) {
                customer.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            Map<String, Double> objTerms = new LinkedHashMap<>();
            for(int i = 1; i < numFacility;i++){
                objTerms.put("x" + i + "" + j, servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
            }
            customer.setObj(objTerms);

            Map<String, Double> ctr1Terms = new LinkedHashMap<>();
            for(int i = 1;i <= numFacility;i++){
                ctr1Terms.put("x" + i + "" + j, 1.0);
            }
            customer.addConstraint("Ctr 1", ctr1Terms, ConstraintType.EQL, 1, 1);

            Map<String, Double> ctr2Terms = new LinkedHashMap<>();
            for(int i = 1;i <= numFacility;i++){
                ctr2Terms.put("x" + i + "" + j, 1.0);
                ctr2Terms.put("y" + i, -1.0);
            }
            customer.addConstraint("Ctr 2", ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 0);

            for(String boundingVar : complicatingVarNames){
                Map<String, Double> boundingTerms = new LinkedHashMap<>();
                boundingTerms.put(boundingVar, 1.0);

                customer.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
                        0 ,0 );
            }

            customer.setSense(ModelSolver.Sense.MIN);
            subSolvers.put("Customer " + j, customer);

        }
    }

    void solveMasterModel(){
        masterSolver.solve();
        lb = masterSolver.getOptimum();
    }

    void solve(){
        initMaster();
        initSubModel();

        solveMasterModel();
        Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();
        solveSubModel(boundingVarSubDuals);
    }

    void solveSubModel(Map<String, Map<String, Double>> boundingVarSubDuals) {
        for (String subProblem : subSolvers.keySet()) {
            for (String boundingVar : complicatingVarNames) {
                subSolvers.get(subProblem).setConstraintBound("Bounding with " +
                                boundingVar, masterSolver.getVariableSol(boundingVar),
                        masterSolver.getVariableSol(boundingVar));
            }

            subSolvers.get(subProblem).solve();

            if (subSolvers.get(subProblem).getStatus() == ModelSolver.Status.OPTIMAL) {
                System.out.println("Sub model objective value: " + subSolvers.get(subProblem).getOptimum());

                for (String boundingVar : complicatingVarNames) {
                    if (!boundingVarSubDuals.containsKey(boundingVar))
                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());

                    boundingVarSubDuals.get(boundingVar).put(subProblem, subSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
                }
            } else {
                System.out.println("Sub model infeasible!");
//                for (String boundingVar : complicatingVarNames) {
//                    feasibleSubSolvers.get(subProblem).setConstraintBound("Bounding with " +
//                                    boundingVar, masterSolver.getVariableSol(boundingVar),
//                            masterSolver.getVariableSol(boundingVar));
//                }
//
//                feasibleSubSolvers.get(subProblem).solve();
//
//                if (feasibleSubSolvers.get(subProblem).getStatus() == ModelSolver.Status.OPTIMAL) {
//                    System.out.println("Sub model objective value: " + feasibleSubSolvers.get(subProblem).getOptimum());
//                } else {
//                    System.out.println("Bug exists when building the alwayse feasible model");
//                }
//
//                for (String boundingVar : complicatingVarNames) {
//                    if (!boundingVarSubDuals.containsKey(boundingVar))
//                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());
//
//                    boundingVarSubDuals.get(boundingVar).put(subProblem, feasibleSubSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
//                }
            }
        }
    }
}
