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
    Map<String, Model> feasibleSubSolvers = new HashMap<>();
    Model masterSolver = new XpressModel("Master");
    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;
    List<String> complicatingVarNames = new ArrayList<>();

    Map<String, Double> openCosts = new LinkedHashMap<>();
    //facility->customer->cost
    Map<String, Map<String, Double>> servingCosts = new LinkedHashMap<>();

    int masterBendersCutId = 0;
    final double LARGE_POSTIVE = 100;

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
//        complicatingVarNames.add("y" + numFacility + 1);  //virtual facility which is always open and has very high opening cost

        for (String locationVar : complicatingVarNames) {
            if(locationVar.equals("y5"))
                masterSolver.addVariable(locationVar, VariableType.BINARY, 1, 1);
            else
                masterSolver.addVariable(locationVar, VariableType.BINARY, 0, 1);
        }
        masterSolver.addVariable("alpha", VariableType.REAL, -10000, Double.MAX_VALUE);

        Map<String, Double> objTerms = new LinkedHashMap<>();
        for (int i = 1; i <= complicatingVarNames.size(); i++) {
            objTerms.put(complicatingVarNames.get(i - 1), openCosts.get(String.valueOf(i)));
        }
        objTerms.put("alpha", 1.0);
        masterSolver.setObj(objTerms);

        masterSolver.setSense(ModelSolver.Sense.MIN);
    }

    void initSubModel() {
        for (int j = 1; j <= numCustomer; j++) {
            Model customer = new XpressModel("Customer " + j);
            for (int i = 1; i <= numFacility; i++) {
                customer.addVariable("x" + i + "" + j, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            for (String boundingVar : complicatingVarNames) {
                customer.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            Map<String, Double> objTerms = new LinkedHashMap<>();
            for (int i = 1; i <= numFacility; i++) {
                objTerms.put("x" + i + "" + j, servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
            }
            customer.setObj(objTerms);

            Map<String, Double> ctr1Terms = new LinkedHashMap<>();
            for (int i = 1; i <= numFacility; i++) {
                ctr1Terms.put("x" + i + "" + j, 1.0);
            }
            customer.addConstraint("Ctr 1", ctr1Terms, ConstraintType.EQL, 1, 1);


            for (int i = 1; i <= numFacility; i++) {
                Map<String, Double> terms = new LinkedHashMap<>();
                terms.put("x" + i + "" + j, 1.0);
                terms.put("y" + i, -1.0);
                customer.addConstraint("Ctr 2 - " + i, terms, ConstraintType.LEQL, -Double.MAX_VALUE, 0);
            }


            for (String boundingVar : complicatingVarNames) {
                Map<String, Double> boundingTerms = new LinkedHashMap<>();
                boundingTerms.put(boundingVar, 1.0);

                customer.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
                        0, 0);
            }

            customer.setSense(ModelSolver.Sense.MIN);
            subSolvers.put("Customer " + j, customer);

        }
    }

    void initFeasibleSubModel() {
        for (int j = 1; j <= numCustomer; j++) {
            Model customer = new XpressModel("Customer " + j);
            for (int i = 1; i <= numFacility; i++) {
                customer.addVariable("x" + i + "" + j, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            for (String boundingVar : complicatingVarNames) {
                customer.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            customer.addVariable("u", VariableType.REAL, 0, Double.MAX_VALUE);

            for (int i = 1; i <= numFacility; i++) {
                customer.addVariable("v" + i, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            customer.addVariable("w", VariableType.REAL, 0, Double.MAX_VALUE);


            Map<String, Double> objTerms = new LinkedHashMap<>();
            for (int i = 1; i <= numFacility; i++) {
                objTerms.put("x" + i + "" + j, servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
                objTerms.put("v" + i, LARGE_POSTIVE);
                objTerms.put("w", LARGE_POSTIVE);
            }
            objTerms.put("u", LARGE_POSTIVE);
            objTerms.put("w", LARGE_POSTIVE);


            customer.setObj(objTerms);

            Map<String, Double> ctr1Terms = new LinkedHashMap<>();
            for (int i = 1; i <= numFacility; i++) {
                ctr1Terms.put("x" + i + "" + j, 1.0);
            }
            ctr1Terms.put("u", 1.0);
            ctr1Terms.put("w", -1.0);
            customer.addConstraint("Ctr 1", ctr1Terms, ConstraintType.EQL, 1, 1);


            for (int i = 1; i <= numFacility; i++) {
                Map<String, Double> terms = new LinkedHashMap<>();
                terms.put("x" + i + "" + j, 1.0);
                terms.put("y" + i, -1.0);
                terms.put("v" + i, 1.0);
                terms.put("w", -1.0);
                customer.addConstraint("Ctr 2 - " + i, terms, ConstraintType.EQL, 0, 0);
            }


            for (String boundingVar : complicatingVarNames) {
                Map<String, Double> boundingTerms = new LinkedHashMap<>();
                boundingTerms.put(boundingVar, 1.0);

                customer.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
                        0, 0);
            }

            customer.setSense(ModelSolver.Sense.MIN);
            feasibleSubSolvers.put("Customer " + j, customer);

        }
    }

    void solveMasterModel() {
        masterSolver.solveMIP();
//        if (masterSolver.getOptimum() > lb)
        lb = masterSolver.getOptimum();

    }

    void updateUB() {
//        ub = 0;

        double currentUb = 0;

        for (int i = 1; i <= numFacility; i++) {
            currentUb += masterSolver.getVariableSol("y" + i) * openCosts.get(String.valueOf(i));

        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                if (subSolvers.get("Customer " + j).getStatus() == ModelSolver.Status.OPTIMAL) {
                    currentUb += subSolvers.get("Customer " + j).getVariableSol("x" + i + "" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                } else {
                    currentUb += feasibleSubSolvers.get("Customer " + j).getVariableSol("x" + i + "" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                }
            }
        }

//        if(currentUb < ub)
        ub = currentUb;
    }

    void addBendersCutToMaster(Map<String, Map<String, Double>> masterVarDuals) {
        double sumOfBoundingMultipliedDual = 0;

        Map<String, Double> cutTerms = new LinkedHashMap<>();

        for (String masterVar : masterVarDuals.keySet()) {
            double masterVarCoeff = 0;
            for (String subProblem : masterVarDuals.get(masterVar).keySet()) {
                masterVarCoeff += masterVarDuals.get(masterVar).get(subProblem);
                sumOfBoundingMultipliedDual += masterVarDuals.get(masterVar).get(subProblem) * masterSolver.getVariableSol(masterVar);
            }
            cutTerms.put(masterVar, masterVarCoeff);
        }
        cutTerms.put("alpha", -1.0);

        double totalSubOptimum = 0;
        for (String subProblem : subSolvers.keySet()) {
            if (subSolvers.get(subProblem).getStatus() == ModelSolver.Status.OPTIMAL)
                totalSubOptimum += subSolvers.get(subProblem).getOptimum();
            else
                totalSubOptimum += feasibleSubSolvers.get(subProblem).getOptimum();
        }

        masterSolver.addConstraint("Benders Cut " + masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
        masterBendersCutId++;
    }

    void solve() {
        initMaster();
        initSubModel();
        initFeasibleSubModel();

        solveMasterModel();
        Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();
        solveSubModel(boundingVarSubDuals);

        updateUB();

        addBendersCutToMaster(boundingVarSubDuals);


        while (Math.abs(ub - lb) >= 1) {
            solveMasterModel();
            solveSubModel(boundingVarSubDuals);
            updateUB();

            addBendersCutToMaster(boundingVarSubDuals);
        }

        System.out.println("Upper bound = " + ub);
        System.out.println("Lower bound = " + lb);
    }

    void solveSubModel(Map<String, Map<String, Double>> boundingVarSubDuals) {
        for (String subProblem : subSolvers.keySet()) {
            for (String boundingVar : complicatingVarNames) {
                subSolvers.get(subProblem).setConstraintBound("Bounding with " +
                                boundingVar, masterSolver.getVariableSol(boundingVar),
                        masterSolver.getVariableSol(boundingVar));
            }

            subSolvers.get(subProblem).solveLP();

            if (subSolvers.get(subProblem).getStatus() == ModelSolver.Status.OPTIMAL) {
                System.out.println("Sub model objective value: " + subSolvers.get(subProblem).getOptimum());

                for (String boundingVar : complicatingVarNames) {
                    if (!boundingVarSubDuals.containsKey(boundingVar))
                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());

                    boundingVarSubDuals.get(boundingVar).put(subProblem, subSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
                }
            } else {
                System.out.println("Sub model infeasible!");
                for (String boundingVar : complicatingVarNames) {
                    feasibleSubSolvers.get(subProblem).setConstraintBound("Bounding with " +
                                    boundingVar, masterSolver.getVariableSol(boundingVar),
                            masterSolver.getVariableSol(boundingVar));
                }

                feasibleSubSolvers.get(subProblem).solveLP();

                if (feasibleSubSolvers.get(subProblem).getStatus() == ModelSolver.Status.OPTIMAL) {
                    System.out.println("Sub model objective value: " + feasibleSubSolvers.get(subProblem).getOptimum());
                } else {
                    System.out.println("Bug exists when building the always feasible model");
                }

                for (String boundingVar : complicatingVarNames) {
                    if (!boundingVarSubDuals.containsKey(boundingVar))
                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());

                    boundingVarSubDuals.get(boundingVar).put(subProblem, feasibleSubSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
                }
            }
        }
    }
}
