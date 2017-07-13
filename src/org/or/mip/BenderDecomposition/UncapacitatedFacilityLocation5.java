package org.or.mip.BenderDecomposition;

import org.or.mip.Modelling.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by baohuaw on 7/5/17.
 * Use GA to obtain a heuristic initial solution.
 * Experiments shows that this way works for problem size less than 200. Much faster, less cuts.
 * But for problem size larger than 250, this way can get a better lower bound, but still difficult to converge
 */
public class UncapacitatedFacilityLocation5 {
    int numFacility;
    int numCustomer;
    Map<String, Model> subSolvers = new HashMap<>();
    Map<String, Model> feasibleSubSolvers = new HashMap<>();
    Model masterSolver = new XpressModel("Master");

    Model originalSolver = new XpressModel("Original");
    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;
    List<String> complicatingVarNames = new ArrayList<>();

    Map<Integer, Double> openCosts = new LinkedHashMap<>();
    //facility->customer->cost
    Map<Integer, Map<Integer, Double>> servingCosts = new LinkedHashMap<>();

    int masterBendersCutId = 0;

    List<Integer> initSolution;

    final int ALPHA_LB = 0;


    public static void main(String[] args) throws IOException {
        String fileName = "/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/KoerkelGhosh-sym/500/a/gs500a-2";

//        String fileName = "/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/kmedian/500-10";

        ApacheGASolver gaSolver = new ApacheGASolver(1000, 100);
        gaSolver.readProblem(fileName);
        List<Integer> initSolution = gaSolver.solve();


        UncapacitatedFacilityLocation5 location = new UncapacitatedFacilityLocation5();
        location.initSolution = initSolution;
        location.readProblem(fileName);
//       location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/KoerkelGhosh-sym/500/a/gs500a-1");
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/simpleExample.txt");
        long startTime = System.currentTimeMillis();
//        location.solveOriginalModel();
        System.out.println("Start Benders Decomposition");
        location.solve();
        System.out.println("Solving the problem with " + (System.currentTimeMillis() - startTime) + " milli second");
    }

    void readSolution(List<Integer> solution) {

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
                    openCosts.put(i, 0.0);
                    if (!servingCosts.containsKey(i)) {
                        servingCosts.put(i, new LinkedHashMap<>());
                    }
                    for (int j = 1; j <= numCustomer; j++) {
                        servingCosts.get(i).put(j, 0.0);
                    }
                }
            } else {
                String[] elements = line.split(" ");
                int facilityId = Integer.valueOf(elements[0]);
                double openCost = Double.valueOf(elements[1]);
                openCosts.put(facilityId, openCost);
                for (int j = 1; j <= elements.length - 2; j++) {
                    servingCosts.get(facilityId).put(j, Double.valueOf(elements[j - 1 + 2]));
                }
            }
            lineId++;
        }
        in.close();
    }

    void initMaster() {

//        Map<String, Double> varValues = new LinkedHashMap<>();
        for (int i = 1; i <= numFacility; i++) {
            complicatingVarNames.add("y_" + i);

//            if (initSolution.get(i - 1) == 1) {
//                varValues.put("y_" + i, 1.0);
//            } else {
//                varValues.put("y_" + i, 0.0);
//            }
        }

        for (String locationVar : complicatingVarNames) {
            masterSolver.addVariable(locationVar, VariableType.INTEGER, 0, 1);
//            masterSolver.addVariable(locationVar, VariableType.INTEGER, varValues.get(locationVar),
//                    varValues.get(locationVar));
        }
        masterSolver.addVariable("alpha", VariableType.REAL, ALPHA_LB, Double.MAX_VALUE);

        Map<String, Double> objTerms = new LinkedHashMap<>();
        for (int i = 1; i <= complicatingVarNames.size(); i++) {
            objTerms.put(complicatingVarNames.get(i - 1), openCosts.get(i));
        }
        objTerms.put("alpha", 1.0);
        masterSolver.setObj(objTerms);

        Map<String, Double> ctrTerms = new LinkedHashMap<>();
        for (int i = 1; i <= complicatingVarNames.size(); i++) {
            ctrTerms.put(complicatingVarNames.get(i - 1), 1.0);
        }
        masterSolver.addConstraint("Facility existence", ctrTerms, ConstraintType.GEQL, 1, Double.MAX_VALUE);

        masterSolver.setSense(Model.Sense.MIN);


//        for(int i = 0;i < complicatingVarNames.size();i++){
//            masterSolver.setVariableBound(complicatingVarNames.get(i), varValues.get(complicatingVarNames.get(i)),
//                    varValues.get(complicatingVarNames.get(i)));
//        }
//        masterSolver.addInitialSolution(varValues);
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
            objTerms.put("y_" + i, openCosts.get(i));
        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                objTerms.put("x_" + i + "_" + j, servingCosts.get(i).get(j));
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
                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(i));
//                totalCost += openCosts.get(i);
            }
        }

        for (int j = 1; j <= numCustomer; j++) {
            for (int i = 1; i < numFacility; i++) {
                if (Math.abs(originalSolver.getVariableSol("x_" + i + "_" + j) - 1) < 0.0001) {
                    System.out.println("Customer " + j + " is served by Facility " + i + " with cost of " + servingCosts.get(i).get(j));
//                    totalCost += servingCosts.get(i).get(j);
                }
            }
        }

        System.out.println("Origin Model Optimum " + originalSolver.getOptimum());

//        for (int i = 1; i <= numFacility; i++) {
//            if (Math.abs(originalSolver.getVariableSol("y_" + i) - 1) <= 0.0001)
//                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(i));
//        }
//
//        for (int i = 1; i < numFacility; i++) {
//            for (int j = 1; j <= numCustomer; j++) {
//                if (Math.abs(originalSolver.getVariableSol("x_" + i + "_" + j) - 1) < 0.0001)
//                    System.out.println("Facility " + i + " serves Customer " + j + " with cost of " + servingCosts.get(i).get(j));
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
            objTerms.put("y_" + i, openCosts.get(i));
        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                objTerms.put("x_" + i + "_" + j, servingCosts.get(i).get(j));
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
//                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(i));
//        }
//
//        for (int i = 1; i < numFacility; i++) {
//            for (int j = 1; j <= numCustomer; j++) {
//                if (Math.abs(originalSolver.getVariableSol("x_" + i + "_" + j) - 1) < 0.0001)
//                    System.out.println("Facility " + i + " serves Customer " + j + " with cost of " + servingCosts.get(i).get(j));
//            }
//
//        }

    }

    void initSubModel() {
        for (int j = 1; j <= numCustomer; j++) {
            Model customer = new XpressModel("Customer " + j);
            for (int i = 1; i <= numFacility; i++) {
                customer.addVariable("x_" + i + "_" + j, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            for (String boundingVar : complicatingVarNames) {
                customer.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
            }

            Map<String, Double> objTerms = new LinkedHashMap<>();
            for (int i = 1; i <= numFacility; i++) {
                objTerms.put("x_" + i + "_" + j, servingCosts.get(i).get(j));
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
//                objTerms.put("x_" + i + "_" + j, servingCosts.get(i).get(j));
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
//            customer.setSense(Model.Sense.MIN);
//            feasibleSubSolvers.put("Customer " + j, customer);
//
//        }
//    }

    protected void solveMasterModel() {
        masterSolver.solveMIP();

//        for (int i = 1; i <= numFacility; i++) {
//            if (Math.abs(masterSolver.getVariableSol("y_" + i) - 1) <= 0.0001) {
//                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(i));
//            }
//        }
//        System.out.println("Time for Solving Master : " + (System.currentTimeMillis() - time));
//        if (masterSolver.getOptimum() > lb)
        lb = masterSolver.getOptimum();

    }

    void updateUB(boolean useHeuristicInput) {
//        ub = 0;

        double currentUb = 0;

        for (int i = 1; i <= numFacility; i++) {

            if (useHeuristicInput) {
                currentUb += initSolution.get(i - 1) * openCosts.get(i);
            } else {
                currentUb += masterSolver.getVariableSol("y_" + i) * openCosts.get(i);
            }
        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                currentUb += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(i).get(j);
//                if (subSolvers.get("Customer " + j).getStatus() == Model.Status.OPTIMAL) {
//
//                } else {
//                    currentUb += feasibleSubSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(i).get(j);
//                }
            }
        }

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


    protected void solve() {
        initMaster();
        initSubModel();

//        solveMasterModel();

//        for (int i = 0; i < complicatingVarNames.size(); i++) {
//            masterSolver.setVariableBound(complicatingVarNames.get(i), 0, 1);
//        }

//        double currentUb = 0;
        lb = 0;
        for (int i = 1; i <= numFacility; i++) {
            lb += initSolution.get(i - 1) * openCosts.get(i);
        }
        lb += ALPHA_LB;

        Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();
        if (!solveSubModel(boundingVarSubDuals, true)) {
            System.out.println("Terminate due to sub problem infeasibility!");
            return;
        }


        updateUB(true);
        addBendersCutToMaster(boundingVarSubDuals);

        int step = 0;


        while (Math.abs(ub - lb) >= 1) {
            if (step % 1 == 0) {
                System.out.println(step + ",  " + lb + ",  " + ub + ",  " + (ub - lb));
            }
            solveMasterModel();
            if (!solveSubModel(boundingVarSubDuals, false)) {
                System.out.println("Terminate due to sub problem infeasibility!");
                return;
            }
            updateUB(false);

            addBendersCutToMaster(boundingVarSubDuals);
            step++;
        }

        System.out.println("Upper bound = " + ub);
        System.out.println("Lower bound = " + lb);

        double totalCost = 0;
        for (int i = 1; i <= numFacility; i++) {
            if (Math.abs(masterSolver.getVariableSol("y_" + i) - 1) <= 0.0001) {
                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(i));
                totalCost += openCosts.get(i);
            }
        }

        for (int j = 1; j <= numCustomer; j++) {
            for (int i = 1; i <= numFacility; i++) {
                if (Math.abs(subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) - 1) < 0.0001) {
                    System.out.println("Customer " + j + " is served by Facility " + i + " with cost of " + servingCosts.get(i).get(j));
                    totalCost += servingCosts.get(i).get(j);
                }
            }
        }

        System.out.println("Total cost " + (totalCost));
        System.out.println("Benders Cut " + masterBendersCutId);
    }

    boolean solveSubModel(Map<String, Map<String, Double>> boundingVarSubDuals, boolean useHeuristicInput) {
        long time = System.currentTimeMillis();
        for (String subProblem : subSolvers.keySet()) {
            for (String boundingVar : complicatingVarNames) {
                if (useHeuristicInput) {
                    String varIndex = boundingVar.substring(boundingVar.indexOf("_") + 1, boundingVar.length());

                    subSolvers.get(subProblem).setConstraintBound("Bounding with " + boundingVar,
                            initSolution.get(Integer.valueOf(varIndex) - 1),
                            initSolution.get(Integer.valueOf(varIndex) - 1));
                } else {
                    subSolvers.get(subProblem).setConstraintBound("Bounding with " + boundingVar,
                            masterSolver.getVariableSol(boundingVar),
                            masterSolver.getVariableSol(boundingVar));
                }

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
        }
//        System.out.println("Time for Solving all Sub : " + (System.currentTimeMillis() - time));
        return true;
    }
}
