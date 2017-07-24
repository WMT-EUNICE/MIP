package org.or.mip.BenderDecomposition;

import org.or.mip.Modelling.ConstraintType;
import org.or.mip.Modelling.Model;
import org.or.mip.Modelling.VariableType;
import org.or.mip.Modelling.XpressModel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by baohuaw on 7/5/17.
 * How about not setting explicit bounding constraint in sub problem? Works
 * Each sub problem generates a cut, ref. paper "redesigning Benders Decomposition for large-scale facility location"
 */
public class UncapacitatedFacilityLocation7 {
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

    int masterBendersCutId = 1;

    Map<Integer, String> bendersCuts = new LinkedHashMap<>();


    Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();

    //    List<Map<Integer, Integer>> cutomerCriticals = new LinkedList<>();
    Map<Integer, List<Integer>> customerCriticals = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        UncapacitatedFacilityLocation7 location = new UncapacitatedFacilityLocation7();
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/GalvaoRaggi/50/50.1");
        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/GalvaoRaggi/200/200.1");
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/KoerkelGhosh-sym/500/a/gs500a-1");
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/kmedian/500-10");
//        location.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/simpleExample2.txt");
//        long startTime = System.currentTimeMillis();
//        location.solveOriginalModel();
        location.solve();
//        System.out.println("Solving the problem with " + (System.currentTimeMillis() - startTime) + " milli second");
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
        for (int i = 1; i <= numFacility; i++) {
            complicatingVarNames.add("y_" + i);
        }

        for (String locationVar : complicatingVarNames) {
            masterSolver.addVariable(locationVar, VariableType.INTEGER, 0, 1);
//            masterSolver.addVariable("alpha_" + locationVar, VariableType.REAL, 0, Double.MAX_VALUE);

        }

        for (int j = 1; j <= numCustomer; j++) {
            masterSolver.addVariable("alpha_" + j, VariableType.REAL, 0, Double.MAX_VALUE);
        }


        Map<String, Double> objTerms = new LinkedHashMap<>();
        for (int i = 1; i <= complicatingVarNames.size(); i++) {
            objTerms.put(complicatingVarNames.get(i - 1), openCosts.get(i));
//            objTerms.put("alpha_y_" + i, 1.0);
        }

        for (int j = 1; j <= numCustomer; j++) {
            objTerms.put("alpha_" + j, 1.0);
        }


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

            if (Math.abs(masterSolver.getVariableSol("y_" + i) - 1) <= 0.0001) {
                currentUb += masterSolver.getVariableSol("y_" + i) * openCosts.get(i);
            }

//            System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(i) + "    " + masterSolver.getVariableSol("y_" + i));
//                totalCost += openCosts.get(i);
        }


        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {

                if(Math.abs(subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) - 1) <= 0.0001) {
                    currentUb += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(i).get(j);
                }
            }
        }

//        printResult();

        ub = currentUb;
    }

    void addBendersCutToMaster() {
        double sumOfBoundingMultipliedDual = 0;

        Map<String, Double> cutTerms = new LinkedHashMap<>();

        for (String masterVar : boundingVarSubDuals.keySet()) {
            double masterVarCoeff = 0;
            for (String subProblem : boundingVarSubDuals.get(masterVar).keySet()) {
                masterVarCoeff += boundingVarSubDuals.get(masterVar).get(subProblem);
                sumOfBoundingMultipliedDual += boundingVarSubDuals.get(masterVar).get(subProblem) * masterSolver.getVariableSol(masterVar);
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


    void addBendersCutToMaster2() {
        for (int j = 1; j <= numCustomer; j++) {
            double sumOfBoundingMultipliedDual = 0;

            Map<String, Double> cutTerms = new LinkedHashMap<>();


            for (String masterVar : boundingVarSubDuals.keySet()) {
                sumOfBoundingMultipliedDual += boundingVarSubDuals.get(masterVar).get("Customer " + j) * masterSolver.getVariableSol(masterVar);

                if(boundingVarSubDuals.get(masterVar).get("Customer " + j) != 0) {
                    cutTerms.put(masterVar, boundingVarSubDuals.get(masterVar).get("Customer " + j));
                }
            }
            cutTerms.put("alpha_" + j, 1.0);

            masterSolver.addConstraint("Benders Cut " + j + " " + masterBendersCutId, cutTerms, ConstraintType.GEQL,
                    subSolvers.get("Customer " + j).getOptimum() + sumOfBoundingMultipliedDual, Double.MAX_VALUE);
            masterBendersCutId++;
        }
    }

    boolean addBendersCutForEachSubProblemToMaster() {
        boolean newCut = false;
        for (int j = 1; j <= numCustomer; j++) {
            Map<Integer, Double> targetServingCosts = new TreeMap<>();
            for (int i = 1; i <= numFacility; i++) {
                targetServingCosts.put(i, servingCosts.get(i).get(j));
            }

            // 升序比较器
            Comparator<Map.Entry<Integer, Double>> valueComparator = (o1, o2) -> {
                // TODO Auto-generated method stub
                return (int) (o1.getValue() - o2.getValue());
            };

            List<Map.Entry<Integer, Double>> list = new ArrayList<>(targetServingCosts.entrySet());
            list.sort(valueComparator);

            int temp = 0;

            int critical = -1;
            Map<String, Double> cutTerms = new LinkedHashMap<>();
            for (Map.Entry<Integer, Double> entry : list) {
                temp += masterSolver.getVariableSol("y_" + entry.getKey());
                if (temp >= 1 && temp - masterSolver.getVariableSol("y_" + entry.getKey()) < 1) {
                    critical = entry.getKey();
                    break;
                }
            }

            List<Integer> historicalCriticals = customerCriticals.get(j);
            if (historicalCriticals != null && historicalCriticals.contains(critical))
                continue;

            if (historicalCriticals == null)
                customerCriticals.put(j, new LinkedList<>());
            customerCriticals.get(j).add(critical);

            for (Map.Entry<Integer, Double> entry : list) {
                if (entry.getKey() == critical) {
                    break;
                } else {
                    if (servingCosts.get(critical).get(j) - entry.getValue() != 0) {
                        cutTerms.put("y_" + entry.getKey(), servingCosts.get(critical).get(j) - entry.getValue());
                    }
                }
            }
            cutTerms.put("alpha_" + j, 1.0);


//            for()
            masterSolver.addConstraint("Benders cut " + j + " " + masterBendersCutId, cutTerms, ConstraintType.GEQL, servingCosts.get(critical).get(j), Double.MAX_VALUE);
            newCut = true;
        }
        return newCut;
    }

    void printResult(){
        for (int i = 1; i <= numFacility; i++) {
            if (Math.abs(masterSolver.getVariableSol("y_" + i) - 1) <= 0.0001) {
                System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(i));
            }
        }

        for (int j = 1; j <= numCustomer; j++) {
            for (int i = 1; i <= numFacility; i++) {
                if (Math.abs(subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) - 1) < 0.0001) {
                    System.out.println("Customer " + j + " is served by Facility " + i + " with cost of " + servingCosts.get(i).get(j));
                }
            }
        }
    }

    protected void solve() {


        initMaster();
        initSubModel();

        long start = System.currentTimeMillis();
        solveMasterModel();
        solveSubModel();

        updateUB();
//        addBendersCutToMaster2();
        if (!addBendersCutForEachSubProblemToMaster()) {
            System.out.println("UB = " + ub);
            return;
        }

        int step = 1;


        while (Math.abs(ub - lb) >= 1) {

            if (step % 1 == 0) {
                System.out.println(step + ",  " + lb + ",  " + ub + ",  " + (ub - lb));
            }

            solveMasterModel();
            solveSubModel();

            updateUB();
//            addBendersCutToMaster();
//            addBendersCutForEachSubProblemToMaster();
            if (!addBendersCutForEachSubProblemToMaster()) {
                System.out.println("UB = " + ub);
                break;
            }
//            addBendersCutToMaster2();
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
        System.out.println("Time " + (System.currentTimeMillis() - start));

        masterSolver.destroy();

        for (String subModel : subSolvers.keySet()) {
            subSolvers.get(subModel).destroy();
        }
    }

    void removeSlackCut() {
//        for(int cut = 1;cut < masterBendersCutId;cut++){
//            if(!masterSolver.hasConstraint("Benders Cut " + cut))
//                continue;
//            if(masterSolver.getSlack("Benders Cut " + cut) > 0){
//                masterSolver.removeConstraint("Benders Cut " + cut);
//
//            }
//        }
        for (Iterator<Integer> iterator = bendersCuts.keySet().iterator(); iterator.hasNext(); ) {
            int cutId = iterator.next();
            if (masterSolver.getSlack(bendersCuts.get(cutId)) > 0) {
                masterSolver.removeConstraint(bendersCuts.get(cutId));
                iterator.remove();
            }
        }
    }

    boolean solveSubModel() {
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
}
