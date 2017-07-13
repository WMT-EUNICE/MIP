package org.or.mip.BenderDecomposition;

import org.or.mip.Modelling.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by baohuaw on 7/5/17.
 * Node that the approache which creates artificial variables to make all sub problems always feasible (ref.
 * Decomposition techniques in Integer programming) does not work for UFL and currently I do not know why.
 * Instead, I create a virtual facility which has very high opening cost and serving cost for each customer and that works
 * , and further, easily to explain and we can decrease the sub problem to half as before
 */
public class UncapacitatedFacilityLocation2 {
    int numFacility;
    int numCustomer;
    Map<String, Model> subSolvers = new HashMap<>();
    Map<String, Model> feasibleSubSolvers = new HashMap<>();
    Model masterSolver = new GoogleMIPModel("Master");

    Model originalSolver = new GoogleMIPModel("Original");
    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;
    List<String> complicatingVarNames = new ArrayList<>();

    Map<String, Double> openCosts = new LinkedHashMap<>();
    //facility->customer->cost
    Map<String, Map<String, Double>> servingCosts = new LinkedHashMap<>();

    int masterBendersCutId = 0;
    final double LARGE_POSTIVE = 100;

    final double LARGE_COST = 200000.0;

    String VIRTUAL_FACILITY;

    public static void main(String[] args) throws IOException {
        UncapacitatedFacilityLocation2 location = new UncapacitatedFacilityLocation2();
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/GalvaoRaggi/200/200.9");
        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/KoerkelGhosh-sym/250/a/gs250a-2");
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/simpleExample.txt");
        long startTime = System.currentTimeMillis();
//        location.solveOriginalModel();
        location.solve();
        System.out.println("Solving the problem with " + (System.currentTimeMillis() - startTime) + " milli second");
    }


    void createVirtualFacility() {
//        int virtualFacilityId = numFacility + 1;
        numFacility++;
        VIRTUAL_FACILITY = String.valueOf(numFacility);
        openCosts.put(String.valueOf(numFacility), LARGE_COST);
        for (int j = 1; j <= numCustomer; j++) {
            if (!servingCosts.containsKey(String.valueOf(numFacility)))
                servingCosts.put(String.valueOf(numFacility), new LinkedHashMap<>());
            servingCosts.get(String.valueOf(numFacility)).put(String.valueOf(j), LARGE_COST);
        }
    }

    void readProblem(String fileName) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String line;
        int lineId = 0;
        while ((line = in.readLine()) != null) {
//            System.out.println(line);
            if (lineId == 0) {
                System.out.println(line);
            } else if (lineId == 1) {
                String[] elements = line.split(" ");
                numFacility = Integer.valueOf(elements[0]);
                numCustomer = Integer.valueOf(elements[1]);
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
            complicatingVarNames.add("y_" + i);
        }

        for (String locationVar : complicatingVarNames) {
            if (locationVar.equals("y_" + VIRTUAL_FACILITY))
                masterSolver.addVariable(locationVar, VariableType.INTEGER, 1, 1);
            else
                masterSolver.addVariable(locationVar, VariableType.INTEGER, 0, 1);
        }
        masterSolver.addVariable("alpha", VariableType.REAL, -10000, Double.MAX_VALUE);

        Map<String, Double> objTerms = new LinkedHashMap<>();
        for (int i = 1; i <= complicatingVarNames.size(); i++) {
            objTerms.put(complicatingVarNames.get(i - 1), openCosts.get(String.valueOf(i)));
        }
        objTerms.put("alpha", 1.0);
        masterSolver.setObj(objTerms);

        masterSolver.setSense(Model.Sense.MIN);
    }

    void solveOriginalModel() {
        for (int i = 1; i <= numFacility; i++) {
            originalSolver.addVariable("y_" + i, VariableType.REAL, 0, 1);
        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                originalSolver.addVariable("x_" + i + "_" + j, VariableType.REAL, 0, 1);
            }
        }

        Map<String, Double> objTerms = new LinkedHashMap<>();
        for (int i = 1; i <= numFacility; i++) {
            objTerms.put("y_" + i, openCosts.get(String.valueOf(i)));
        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                objTerms.put("x_" + i + "_" + j, servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
            }
        }
        originalSolver.setObj(objTerms);

        for (int j = 1; j <= numCustomer; j++) {
            Map<String, Double> terms = new LinkedHashMap<>();
            for (int i = 1; i < numFacility; i++) {
                terms.put("x_" + i + "_" + j, 1.0);
            }
            originalSolver.addConstraint("customer " + j + " serving", terms, ConstraintType.EQL, 1, 1);
        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                Map<String, Double> terms = new LinkedHashMap<>();
                terms.put("x_" + i + "_" + j, 1.0);
                terms.put("y_" + i, -1.0);
                originalSolver.addConstraint("customer " + j + " facility " + i + " compatible", terms, ConstraintType.LEQL, -Double.MAX_VALUE, 0);
            }
        }

        originalSolver.setSense(Model.Sense.MIN);
        originalSolver.solveMIP();



        for (int i = 1; i <= numFacility; i++) {
            if (Math.abs(originalSolver.getVariableSol("y_" + i) - 1) <= 0.0001) {
                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(String.valueOf(i)));
//                totalCost += openCosts.get(String.valueOf(i));
            }
        }

        for (int j = 1; j <= numCustomer; j++) {
            for (int i = 1; i < numFacility; i++) {
                if (Math.abs(originalSolver.getVariableSol("x_" + i + "_" + j) - 1) < 0.0001) {
                    System.out.println("Customer " + j + " is served by Facility " + i + " with cost of " + servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
//                    totalCost += servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                }
            }
        }

        System.out.println("Origin Model Optimum " + originalSolver.getOptimum());

//        for (int i = 1; i <= numFacility; i++) {
//            if (Math.abs(originalSolver.getVariableSol("y_" + i) - 1) <= 0.0001)
//                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(String.valueOf(i)));
//        }
//
//        for (int i = 1; i < numFacility; i++) {
//            for (int j = 1; j <= numCustomer; j++) {
//                if (Math.abs(originalSolver.getVariableSol("x_" + i + "_" + j) - 1) < 0.0001)
//                    System.out.println("Facility " + i + " serves Customer " + j + " with cost of " + servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
//            }
//
//        }

    }

    void solveOriginalModelWithWeakerBound() {
        for (int i = 1; i <= numFacility; i++) {
            originalSolver.addVariable("y_" + i, VariableType.BINARY, 0, 1);
        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                originalSolver.addVariable("x_" + i + "_" + j, VariableType.BINARY, 0, 1);
            }
        }

        Map<String, Double> objTerms = new LinkedHashMap<>();
        for (int i = 1; i <= numFacility; i++) {
            objTerms.put("y_" + i, openCosts.get(String.valueOf(i)));
        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                objTerms.put("x_" + i + "_" + j, servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
            }
        }
        originalSolver.setObj(objTerms);

        for (int j = 1; j <= numCustomer; j++) {
            Map<String, Double> terms = new LinkedHashMap<>();
            for (int i = 1; i < numFacility; i++) {
                terms.put("x_" + i + "_" + j, 1.0);
            }
            originalSolver.addConstraint("customer " + j + " serving", terms, ConstraintType.EQL, 1, 1);
        }

        for (int i = 1; i <= numFacility; i++) {
            Map<String, Double> terms = new LinkedHashMap<>();
            for (int j = 1; j <= numCustomer; j++) {
                terms.put("x_" + i + "_" + j, 1.0);
            }

            terms.put("y_" + i, (double) -numCustomer);
            originalSolver.addConstraint("Weaker bound facility " + i, terms, ConstraintType.LEQL, -Double.MAX_VALUE, 0);
        }

        originalSolver.setSense(Model.Sense.MIN);
        originalSolver.solveMIP();

        System.out.println("Origin Model Optimum " + originalSolver.getOptimum());

//        for (int i = 1; i <= numFacility; i++) {
//            if (Math.abs(originalSolver.getVariableSol("y_" + i) - 1) <= 0.0001)
//                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(String.valueOf(i)));
//        }
//
//        for (int i = 1; i < numFacility; i++) {
//            for (int j = 1; j <= numCustomer; j++) {
//                if (Math.abs(originalSolver.getVariableSol("x_" + i + "_" + j) - 1) < 0.0001)
//                    System.out.println("Facility " + i + " serves Customer " + j + " with cost of " + servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
//            }
//
//        }

    }

    void initSubModel() {
        for (int j = 1; j <= numCustomer; j++) {
            Model customer = new GoogleLPModel("Customer " + j);
            for (int i = 1; i <= numFacility; i++) {
                customer.addVariable("x_" + i + "_" + j, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            for (String boundingVar : complicatingVarNames) {
                customer.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            Map<String, Double> objTerms = new LinkedHashMap<>();
            for (int i = 1; i <= numFacility; i++) {
                objTerms.put("x_" + i + "_" + j, servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
            }
            customer.setObj(objTerms);

            Map<String, Double> ctr1Terms = new LinkedHashMap<>();
            for (int i = 1; i <= numFacility; i++) {
                ctr1Terms.put("x_" + i + "_" + j, 1.0);
            }
            customer.addConstraint("Ctr 1", ctr1Terms, ConstraintType.EQL, 1, 1);


            for (int i = 1; i <= numFacility; i++) {
                Map<String, Double> terms = new LinkedHashMap<>();
                terms.put("x_" + i + "_" + j, 1.0);
                terms.put("y_" + i, -1.0);
                customer.addConstraint("Ctr 2 - " + i, terms, ConstraintType.LEQL, -Double.MAX_VALUE, 0);
            }


            for (String boundingVar : complicatingVarNames) {
                Map<String, Double> boundingTerms = new LinkedHashMap<>();
                boundingTerms.put(boundingVar, 1.0);

                customer.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
                        0, 0);
            }

            customer.setSense(Model.Sense.MIN);
            subSolvers.put("Customer " + j, customer);

        }
    }

//    void initFeasibleSubModel() {
//        for (int j = 1; j <= numCustomer; j++) {
//            Model customer = new XpressModel("Customer " + j);
//            for (int i = 1; i <= numFacility; i++) {
//                customer.addVariable("x_" + i + "_" + j, VariableType.REAL, 0, Double.MAX_VALUE);
//            }
//
//            for (String boundingVar : complicatingVarNames) {
//                customer.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
//            }
//
//            customer.addVariable("u", VariableType.REAL, 0, Double.MAX_VALUE);
//
//            for (int i = 1; i <= numFacility; i++) {
//                customer.addVariable("v" + i, VariableType.REAL, 0, Double.MAX_VALUE);
//            }
//
//            customer.addVariable("w", VariableType.REAL, 0, Double.MAX_VALUE);
//
//
//            Map<String, Double> objTerms = new LinkedHashMap<>();
//            for (int i = 1; i <= numFacility; i++) {
//                objTerms.put("x_" + i + "_" + j, servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
//                objTerms.put("v" + i, LARGE_POSTIVE);
//                objTerms.put("w", LARGE_POSTIVE);
//            }
//            objTerms.put("u", LARGE_POSTIVE);
//            objTerms.put("w", LARGE_POSTIVE);
//
//
//            customer.setObj(objTerms);
//
//            Map<String, Double> ctr1Terms = new LinkedHashMap<>();
//            for (int i = 1; i <= numFacility; i++) {
//                ctr1Terms.put("x_" + i + "_" + j, 1.0);
//            }
//            ctr1Terms.put("u", 1.0);
//            ctr1Terms.put("w", -1.0);
//            customer.addConstraint("Ctr 1", ctr1Terms, ConstraintType.EQL, 1, 1);
//
//
//            for (int i = 1; i <= numFacility; i++) {
//                Map<String, Double> terms = new LinkedHashMap<>();
//                terms.put("x_" + i + "_" + j, 1.0);
//                terms.put("y_" + i, -1.0);
//                terms.put("v" + i, 1.0);
//                terms.put("w", -1.0);
//                customer.addConstraint("Ctr 2 - " + i, terms, ConstraintType.EQL, 0, 0);
//            }
//
//
//            for (String boundingVar : complicatingVarNames) {
//                Map<String, Double> boundingTerms = new LinkedHashMap<>();
//                boundingTerms.put(boundingVar, 1.0);
//
//                customer.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
//                        0, 0);
//            }
//
//            customer.setSense(ModelSolver.Sense.MIN);
//            feasibleSubSolvers.put("Customer " + j, customer);
//
//        }
//    }

    void solveMasterModel() {
//        masterSolver.solveMIP();
        long time = System.currentTimeMillis();
        masterSolver.solveMIP();
        System.out.println("Time for Solving Master : " + (System.currentTimeMillis() - time));
//        if (masterSolver.getOptimum() > lb)
        lb = masterSolver.getOptimum();

    }

    void updateUB() {
//        ub = 0;

        double currentUb = 0;

        for (int i = 1; i <= numFacility; i++) {
            currentUb += masterSolver.getVariableSol("y_" + i) * openCosts.get(String.valueOf(i));

        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                currentUb += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
//                if (subSolvers.get("Customer " + j).getStatus() == ModelSolver.Status.OPTIMAL) {
//
//                } else {
//                    currentUb += feasibleSubSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
//                }
            }
        }

//        if (currentUb < ub)
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
            if (subSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL)
                totalSubOptimum += subSolvers.get(subProblem).getOptimum();
            else
                totalSubOptimum += feasibleSubSolvers.get(subProblem).getOptimum();
        }

        masterSolver.addConstraint("Benders Cut " + masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
//        masterSolver.addCut(masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
        masterBendersCutId++;
    }

    void solve() {
        createVirtualFacility();

        initMaster();
        initSubModel();

        solveMasterModel();
        Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();
        if (!solveSubModel(boundingVarSubDuals)) {
            System.out.println("Terminate due to sub problem infeasibility!");
            return;
        }


        updateUB();
        addBendersCutToMaster(boundingVarSubDuals);

        int step = 0;


        while (Math.abs(ub - lb) >= 1) {

            if (step % 1 == 0) {
                System.out.println("### Step " + step);
                System.out.println("UB = " + ub);
                System.out.println("LB = " + lb);
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
            System.out.println("Facility " + i + " : " + masterSolver.getVariableSol("y_" + i));
//            totalCost += openCosts.get(String.valueOf(i));
//            if (Math.abs(masterSolver.getVariableSol("y_" + i) - 1) <= 0.0001) {
//
//            }
        }

        for (int j = 1; j <= numCustomer; j++) {
            for (int i = 1; i < numFacility; i++) {
                if (Math.abs(subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) - 1) < 0.0001) {
                    System.out.println("Customer " + j + " is served by Facility " + i + " with cost of " + servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
                    totalCost += servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                }
            }
        }

        System.out.println("Total cost " + (totalCost - LARGE_COST));
        System.out.println("Benders Cut " + masterBendersCutId);
    }

    boolean solveSubModel(Map<String, Map<String, Double>> boundingVarSubDuals) {
        long time = System.currentTimeMillis();
        for (String subProblem : subSolvers.keySet()) {
            for (String boundingVar : complicatingVarNames) {
                subSolvers.get(subProblem).setConstraintBound("Bounding with " +
                                boundingVar, masterSolver.getVariableSol(boundingVar),
                        masterSolver.getVariableSol(boundingVar));
            }

            subSolvers.get(subProblem).solveLP();

            if (subSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL) {
//                System.out.println("Sub model objective value: " + subSolvers.get(subProblem).getOptimum());

                for (String boundingVar : complicatingVarNames) {
                    if (!boundingVarSubDuals.containsKey(boundingVar))
                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());

                    boundingVarSubDuals.get(boundingVar).put(subProblem, subSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
                }
            } else {
                System.out.println("Sub model infeasible!");
                return false;
            }
//            else {
//                System.out.println("Sub model infeasible!");
//                for (String boundingVar : complicatingVarNames) {
//                    feasibleSubSolvers.get(subProblem).setConstraintBound("Bounding with " +
//                                    boundingVar, masterSolver.getVariableSol(boundingVar),
//                            masterSolver.getVariableSol(boundingVar));
//                }
//
//                feasibleSubSolvers.get(subProblem).solveLP();
//
//                if (feasibleSubSolvers.get(subProblem).getStatus() == ModelSolver.Status.OPTIMAL) {
//                    System.out.println("Sub model objective value: " + feasibleSubSolvers.get(subProblem).getOptimum());
//                } else {
//                    System.out.println("Bug exists when building the always feasible model");
//                }
//
//                for (String boundingVar : complicatingVarNames) {
//                    if (!boundingVarSubDuals.containsKey(boundingVar))
//                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());
//
//                    boundingVarSubDuals.get(boundingVar).put(subProblem, feasibleSubSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
//                }
//            }
        }
        System.out.println("Time for Solving all Sub : " + (System.currentTimeMillis() - time));
        return true;
    }
}
