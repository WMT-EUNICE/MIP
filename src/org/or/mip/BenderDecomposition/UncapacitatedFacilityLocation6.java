package org.or.mip.BenderDecomposition;

import org.or.mip.Modelling.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by baohuaw on 7/5/17.
 * How about not setting explicit bounding constraint in sub problem? Works
 */
public class UncapacitatedFacilityLocation6 {
    int numFacility;
    int numCustomer;
    Map<String, Model> subSolvers = new HashMap<>();
    Map<String, Model> feasibleSubSolvers = new HashMap<>();
    Model masterSolver = new XpressModel("Master");

    Model originalSolver = new XpressModel("Original");
    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;
    List<String> complicatingVarNames = new ArrayList<>();

    Map<String, Double> openCosts = new LinkedHashMap<>();
    //facility->customer->cost
    Map<String, Map<String, Double>> servingCosts = new LinkedHashMap<>();

    int masterBendersCutId = 1;

    Map<Integer, String> bendersCuts = new LinkedHashMap<>();
//    final double LARGE_POSTIVE = 100;

//    final double LARGE_COST = 20000.0;

//    String VIRTUAL_FACILITY;

//    double LAMDA = 0.2;
//
//    Map<String, Double> yy = new LinkedHashMap<>();
//
//    double DELTA = 0.0002;


    public static void main(String[] args) throws IOException {
        UncapacitatedFacilityLocation6 location = new UncapacitatedFacilityLocation6();
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/GalvaoRaggi/50/50.1");
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/GalvaoRaggi/200/200.1");
        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/KoerkelGhosh-sym/250/a/gs250a-2");
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/kmedian/500-10");
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/simpleExample.txt");
        long startTime = System.currentTimeMillis();
//        location.solveOriginalModel();
        location.solve();
        System.out.println("Solving the problem with " + (System.currentTimeMillis() - startTime) + " milli second");
    }

    void readProblem(String fileName) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String line;
        int lineId = 0;
        while ((line = in.readLine()) != null) {
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
            masterSolver.addVariable(locationVar, VariableType.REAL, 0, 1);
        }
        masterSolver.addVariable("alpha", VariableType.REAL, 0, Double.MAX_VALUE);

        Map<String, Double> objTerms = new LinkedHashMap<>();
        for (int i = 1; i <= complicatingVarNames.size(); i++) {
            objTerms.put(complicatingVarNames.get(i - 1), openCosts.get(String.valueOf(i)));
        }
        objTerms.put("alpha", 1.0);
        masterSolver.setObj(objTerms);

        Map<String, Double> ctrTerms = new LinkedHashMap<>();
        for (int i = 1; i <= complicatingVarNames.size(); i++) {
            ctrTerms.put(complicatingVarNames.get(i - 1), 1.0);
        }
        masterSolver.addConstraint("Facility existence", ctrTerms, ConstraintType.GEQL, 1, Double.MAX_VALUE);

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
            Model customer = new XpressModel("Customer " + j);
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
//                terms.put("y_" + i, -1.0);
                customer.addConstraint("Bounding with y_" + i, terms, ConstraintType.LEQL, -Double.MAX_VALUE, 0);
            }


//            for (String boundingVar : complicatingVarNames) {
//                Map<String, Double> boundingTerms = new LinkedHashMap<>();
//                boundingTerms.put(boundingVar, 1.0);
//
//                customer.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
//                        0, 0);
//            }

            customer.setSense(Model.Sense.MIN);
            subSolvers.put("Customer " + j, customer);

        }
    }

    protected void solveMasterModel() {
        masterSolver.solveMIP();
        lb = masterSolver.getOptimum();

    }

    protected void updateUB() {
        double currentUb = 0;

        for (int i = 1; i <= numFacility; i++) {
//            if (Math.abs(masterSolver.getVariableSol("y_" + i) - 1) <= 0.0001) {
//
//            }

            System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(String.valueOf(i)) + "    " + masterSolver.getVariableSol("y_" + i));
//                totalCost += openCosts.get(String.valueOf(i));
            currentUb += masterSolver.getVariableSol("y_" + i) * openCosts.get(String.valueOf(i));



        }

//        for (int i = 1; i <= numFacility; i++) {
//            if (Math.abs(masterSolver.getVariableSol("y_" + i) - 1) <= 0.0001) {
//                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(String.valueOf(i)));
////                totalCost += openCosts.get(String.valueOf(i));
//            }
//        }


        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                currentUb += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
            }
        }

        ub = currentUb;
    }

//    void updateUB(Map<String, Double> separator) {
//        double currentUb = 0;
//
//        for (int i = 1; i <= numFacility; i++) {
////            currentUb += masterSolver.getVariableSol("y_" + i) * openCosts.get(String.valueOf(i));
//            currentUb += separator.get("y_" + i) * openCosts.get(String.valueOf(i));
//
//        }
//
//        for (int i = 1; i <= numFacility; i++) {
//            for (int j = 1; j <= numCustomer; j++) {
//                currentUb += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
//            }
//        }
//
//        ub = currentUb;
//    }

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
        bendersCuts.put(masterBendersCutId, "Benders Cut " + masterBendersCutId);
//        masterSolver.addCut(masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
        masterBendersCutId++;
    }

//    void addBendersCutToMaster(Map<String, Map<String, Double>> masterVarDuals, Map<String, Double> separator) {
//        double sumOfBoundingMultipliedDual = 0;
//
//        Map<String, Double> cutTerms = new LinkedHashMap<>();
//
//        for (String masterVar : masterVarDuals.keySet()) {
//            double masterVarCoeff = 0;
//            for (String subProblem : masterVarDuals.get(masterVar).keySet()) {
//                masterVarCoeff += masterVarDuals.get(masterVar).get(subProblem);
//                sumOfBoundingMultipliedDual += masterVarDuals.get(masterVar).get(subProblem) * separator.get(masterVar);
//            }
//            cutTerms.put(masterVar, masterVarCoeff);
//        }
//        cutTerms.put("alpha", -1.0);
//
//        double totalSubOptimum = 0;
//        for (String subProblem : subSolvers.keySet()) {
//            totalSubOptimum += subSolvers.get(subProblem).getOptimum();
////            if (subSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL)
////                totalSubOptimum += subSolvers.get(subProblem).getOptimum();
////            else
////                totalSubOptimum += feasibleSubSolvers.get(subProblem).getOptimum();
//        }
//
//        masterSolver.addConstraint("Benders Cut " + masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
//        bendersCuts.put(masterBendersCutId, "Benders Cut " + masterBendersCutId);
////        masterSolver.addCut(masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
//        masterBendersCutId++;
//    }

    protected void solve() {
//        createVirtualFacility();

//        double lbNoImprovement = 0;

        initMaster();
        initSubModel();

        solveMasterModel();

//        if (masterSolver.getOptimum() != lb) {
//            lb = masterSolver.getOptimum();
//        } else {
//            lbNoImprovement++;
//        }



        Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();
//        if (!solveSubModel(boundingVarSubDuals)) {
//            System.out.println("Terminate due to sub problem infeasibility!");
//            return;
//        }
//        updateUB();
//        addBendersCutToMaster(boundingVarSubDuals);

//        Map<String, Double> seperator = stabilize();
        if (!solveSubModel(boundingVarSubDuals)) {
            System.out.println("Terminate due to sub problem infeasibility!");
            return;
        }

        updateUB();
//        updateUB(seperator);
        addBendersCutToMaster(boundingVarSubDuals);

        int step = 1;


        while (Math.abs(ub - lb) >= 1) {
//            System.
//            if (step % 1 == 0)

//            if(step % 5 == 0){
//                removeSlackCut();
//            }

            if (step % 1 == 0) {
                System.out.println(step + ",  " + lb + ",  " + ub + ",  " + (ub - lb));
//                System.out.println("UB = " + ub);
//                System.out.println("LB = " + lb);
            }

//            if(step % 5 == 0)
//                removeSlackCut();
            solveMasterModel();

//            if (masterSolver.getOptimum() != lb) {
//                lb = masterSolver.getOptimum();
//            } else {
//                lbNoImprovement++;
//            }

//            if (lbNoImprovement == 5) {
//                LAMDA = 1;
//            }
//
//            if (lbNoImprovement == 10) {
//                DELTA = 0;
//            }
//
//            if (lbNoImprovement == 15) {
//                break;
//            }

//            boundingVarSubDuals = new LinkedHashMap<>();
//            if (!solveSubModel(boundingVarSubDuals)) {
//                System.out.println("Terminate due to sub problem infeasibility!");
//                return;
//            }
//            updateUB();
//            addBendersCutToMaster(boundingVarSubDuals);

//            seperator = stabilize();

            if (!solveSubModel(boundingVarSubDuals)) {
                System.out.println("Terminate due to sub problem infeasibility!");
                return;
            }
//            updateUB();

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
            for (int i = 1; i <= numFacility; i++) {
                if (Math.abs(subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) - 1) < 0.0001) {
                    System.out.println("Customer " + j + " is served by Facility " + i + " with cost of " + servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
                    totalCost += servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                }
            }
        }

        System.out.println("Total cost " + (totalCost));
        System.out.println("Benders Cut " + masterBendersCutId);

        masterSolver.destroy();

        for(String subModel : subSolvers.keySet()){
            subSolvers.get(subModel).destroy();
        }
    }

    void removeSlackCut(){
//        for(int cut = 1;cut < masterBendersCutId;cut++){
//            if(!masterSolver.hasConstraint("Benders Cut " + cut))
//                continue;
//            if(masterSolver.getSlack("Benders Cut " + cut) > 0){
//                masterSolver.removeConstraint("Benders Cut " + cut);
//
//            }
//        }
        for(Iterator<Integer> iterator  = bendersCuts.keySet().iterator();iterator.hasNext();){
            int cutId = iterator.next();
            if(masterSolver.getSlack(bendersCuts.get(cutId)) > 0){
                masterSolver.removeConstraint(bendersCuts.get(cutId));
                iterator.remove();
            }
        }
    }

    boolean solveSubModel(Map<String, Map<String, Double>> boundingVarSubDuals) {
        long time = System.currentTimeMillis();
        for (String subProblem : subSolvers.keySet()) {
            for (String boundingVar : complicatingVarNames) {
                subSolvers.get(subProblem).setConstraintBound("Bounding with " +
                                boundingVar, -Double.MAX_VALUE,
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
        }
//        System.out.println("Time for Solving all Sub : " + (System.currentTimeMillis() - time));
        return true;
    }

//    boolean solveSubModel(Map<String, Map<String, Double>> boundingVarSubDuals, Map<String, Double> separator) {
//        long time = System.currentTimeMillis();
//        for (String subProblem : subSolvers.keySet()) {
//            for (String boundingVar : complicatingVarNames) {
//                subSolvers.get(subProblem).setConstraintBound("Bounding with " +
//                                boundingVar, -Double.MAX_VALUE,
//                        separator.get(boundingVar));
//            }
//
//            subSolvers.get(subProblem).solveLP();
//
//            if (subSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL) {
////                System.out.println("Sub model objective value: " + subSolvers.get(subProblem).getOptimum());
//
//                for (String boundingVar : complicatingVarNames) {
//                    if (!boundingVarSubDuals.containsKey(boundingVar))
//                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());
//
//                    boundingVarSubDuals.get(boundingVar).put(subProblem, subSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
//                }
//            } else {
//                System.out.println("Sub model infeasible!");
//                return false;
//            }
//        }
////        System.out.println("Time for Solving all Sub : " + (System.currentTimeMillis() - time));
//        return true;
//    }
//
//    int findCriticalItem() {
////        int totalLocationVar = 0;
//        int temp = 0;
////        int temp2 = 0;
//        for (int i = 1; i <= numFacility; i++) {
//
//            temp += masterSolver.getVariableSol("y_" + i);
//            if (temp >= 1 && temp - masterSolver.getVariableSol("y_" + i) < 1) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    Map<String, Double> stabilize() {
//        for (String var : complicatingVarNames) {
//            double temp = (yy.get(var) + masterSolver.getVariableSol(var)) * 0.5;
//            yy.put(var, temp);
//        }
//
//        Map<String, Double> separator = new LinkedHashMap<>();
//        for (String var : complicatingVarNames) {
//            double temp = LAMDA * masterSolver.getVariableSol(var) + (1 - LAMDA) * yy.get(var) + DELTA;
////            double temp = LAMDA * masterSolver.getVariableSol(var) + (1 - LAMDA) * yy.get(var);
//            separator.put(var, temp);
//        }
//        return separator;
//    }

//    boolean solveSubModelNoUsingXpress(Map<String, Map<String, Double>> boundingVarSubDuals) {
//        long time = System.currentTimeMillis();
//        for (String subProblem : subSolvers.keySet()) {
//            int critical = findCriticalItem();
//            if(critical == -1){
//                System.out.println("Error in finding critical item");
//                return false;
//            }
//
//            Map<String, Double> x = new HashMap<>();
//
//
////            for (String boundingVar : complicatingVarNames) {
////                subSolvers.get(subProblem).setConstraintBound("Bounding with " +
////                                boundingVar, -Double.MAX_VALUE,
////                        masterSolver.getVariableSol(boundingVar));
////            }
////
////            subSolvers.get(subProblem).solveLP();
////
////            if (subSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL) {
//////                System.out.println("Sub model objective value: " + subSolvers.get(subProblem).getOptimum());
////
////                for (String boundingVar : complicatingVarNames) {
////                    if (!boundingVarSubDuals.containsKey(boundingVar))
////                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());
////
////                    boundingVarSubDuals.get(boundingVar).put(subProblem, subSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
////                }
////            } else {
////                System.out.println("Sub model infeasible!");
////                return false;
////            }
//        }
////        System.out.println("Time for Solving all Sub : " + (System.currentTimeMillis() - time));
//        return true;
//    }
}
