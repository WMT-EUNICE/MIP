package org.or.mip.BenderDecomposition;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.or.mip.BranchBound.BranchAndBound;
import org.or.mip.Modelling.ConstraintType;
import org.or.mip.Modelling.Model;

import java.io.IOException;
import java.util.*;

/**
 * Created by baohuaw on 7/13/17.
 */
public class BranchAndCut extends UncapacitatedFacilityLocation6 {
    public class BranchingConstraint {
        String name;
        String branchingVarName;
        ConstraintType branchingType;
        double bound;

        public BranchingConstraint(String name, String branchingVarName, ConstraintType branchingType, double bound) {
            this.name = name;
            this.branchingVarName = branchingVarName;
            this.branchingType = branchingType;
            this.bound = bound;
        }
    }

    public class BranchingConstraintSet {
        int nodeId;
        List<BranchingConstraint> branchingConstraints = new LinkedList<>();

        public BranchingConstraintSet(int nodeId) {
            this.nodeId = nodeId;
        }
    }

    double LAMDA = 0.2;

    Map<String, Double> yy = new LinkedHashMap<>();

    final double INT_GAP = 0.00001;

    double DELTA = 2 * INT_GAP;

    int nodeNum = 0;

    Map<Integer, String> bendersCuts = new LinkedHashMap<>();

    Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();

    List<BranchingConstraintSet> branchingConsSet = new LinkedList<>();

    public static void main(String[] args) throws IOException {

        String fileName = "/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/KoerkelGhosh-sym/250/a/gs250a-1";
//        String fileName = "/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/KoerkelGhosh-sym/250/a/gs250a-1";
//        ApacheGASolver gaSolver = new ApacheGASolver(1000, 100);
//        gaSolver.readProblem(fileName);
//
//        double heuristicSolution = gaSolver.solveToGetOptimum();


        BranchAndCut bc = new BranchAndCut();
//        bc.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/simpleExample2.txt");

        bc.readProblem(fileName);
//        bc.ub = heuristicSolution;
        bc.branchAndCut();
//        bc.branchAndCut2();
//        bc.branchAndCut3();
    }

    //add benders cut to integer solution node
    void branchAndCut() {
//        for (int i = 1; i <= numFacility; i++) {
//            yy.put("y_" + i, 1.0);
//        }


        initMaster();
        initSubModel();

        long start = System.currentTimeMillis();


        masterSolver.solveLP();
        bendersDecomposition(0);


        nodeNum++;
        BranchingConstraintSet target = new BranchingConstraintSet(0);
        branching(target);

        int step = 1;
        while (!branchingConsSet.isEmpty()) {
            if (step % 50 == 0)
                System.out.println(ub + "   node size " + branchingConsSet.size() + "   step = " + step);
            target = branchingConsSet.get(0);
            branchingConsSet.remove(0);

            for (BranchingConstraint branching : target.branchingConstraints) {
                Map<String, Double> terms = new HashMap<>();
                terms.put(branching.branchingVarName, 1.0);
                if (branching.branchingType == ConstraintType.LEQL)
                    masterSolver.addConstraint(branching.name, terms, branching.branchingType, -Double.MAX_VALUE, branching.bound);
                else
                    masterSolver.addConstraint(branching.name, terms, branching.branchingType, branching.bound, Double.MAX_VALUE);
            }

            masterSolver.solveLP();

            if (masterSolver.getStatus() == Model.Status.OPTIMAL) {
                if (masterSolver.getOptimum() < ub && Math.abs(masterSolver.getOptimum() - ub) >= INT_GAP) {
                    bendersDecomposition(target.nodeId);


                    branching(target);
                }
            }

//            if (masterSolver.getStatus() != Model.Status.OPTIMAL) {
//                for (BranchingConstraint branching : target.branchingConstraints) {
//                    masterSolver.removeConstraint(branching.name);
//                }
//                step++;
//                continue;
//            }
//
//            if (masterSolver.getOptimum() > ub) {
//                for (BranchingConstraint branching : target.branchingConstraints) {
//                    masterSolver.removeConstraint(branching.name);
//                }
//                step++;
//                continue;
//            }
//
//            while (integerSolution()) {
//                if(masterSolver.getOptimum() > ub)
//                    break;
//                lb = masterSolver.getOptimum();
//                solveSubModel();
//                updateUB();
//                addBendersCutToMaster();
//                if (lb == ub) {
//                    System.out.println("find optimal solution");
//                    System.out.println("UB = " + ub);
//                    System.out.println("LB = " + lb);
//                    break;
//                }
//                masterSolver.solveLP();
//            }
////            bendersDecomposition();
//
//            branching(target);
            for (BranchingConstraint branching : target.branchingConstraints) {
                masterSolver.removeConstraint(branching.name);
            }

//            if (step % 5 == 0) {
//                removeSlackCutOfMaster();
//            }
            step++;
        }
        System.out.println("UB = " + ub);
        System.out.println("LB = " + lb);
        System.out.println("Time " + (System.currentTimeMillis() - start) + " Step = " + step);
    }

    void removeSlackCutOfMaster() {
        for (Iterator<Integer> iterator = bendersCuts.keySet().iterator(); iterator.hasNext(); ) {
            int cutId = iterator.next();
            if (masterSolver.getSlack(bendersCuts.get(cutId)) > 0) {
                masterSolver.removeConstraint(bendersCuts.get(cutId));
                iterator.remove();
            }
        }
    }


    //add benders cut at each node, branching when no integer solution is found
    void branchAndCut2() {
        for (int i = 1; i <= numFacility; i++) {
            yy.put("y_" + i, 1.0);
        }

        initMaster();  //init the relaxed LP
        initSubModel();

        long start = System.currentTimeMillis();

        masterSolver.solveLP();
//
//  bendersDecomposition("root");

        int lbNoImprovement = 0;

//        double nodeLB = masterSolver.getOptimum();
        double nodeLB = 0;
        double nodeUB = ub;
        boolean integerFound = false;
        int step = 1;

        if (Math.abs(masterSolver.getOptimum() - nodeLB) >= 1) {
            nodeLB = masterSolver.getOptimum();
        } else {
            lbNoImprovement++;
        }

        if (masterSolver.getOptimum() < ub && Math.abs(masterSolver.getOptimum() - ub) > INT_GAP) {
            while (Math.abs(nodeLB - nodeUB) > INT_GAP) {
                if (step % 1 == 0)
                    System.out.println(step + " , " + nodeLB + " , " + nodeUB + " , " + ub);
                step++;

                Map<String, Double> separator = stabilize();
                solveSubModelWithSeparator(separator);

//                solveSubModel();
//                nodeUB = 0;
//                for (int i = 1; i <= numFacility; i++) {
//                    nodeUB += masterSolver.getVariableSol("y_" + i) * openCosts.get(String.valueOf(i));
////                    nodeUB = separator.get("y_" + i) * openCosts.get(String.valueOf(i));
//                }
//                for (int i = 1; i <= numFacility; i++) {
//                    for (int j = 1; j <= numCustomer; j++) {
//                        nodeUB += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
//                    }
//                }

//                nodeUB = currentUb;
//                updateUB();
//                if (integerSolution()) {
//                    integerFound = true;
////                    for (int i = 1; i <= numFacility; i++) {
////                        if (Math.abs(masterSolver.getVariableSol("y_" + i) - 1) <= INT_GAP) {
////                            System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(String.valueOf(i)));
//////                            totalCost += openCosts.get(String.valueOf(i));
////                        }
////                    }
////
////                    for (int j = 1; j <= numCustomer; j++) {
////                        for (int i = 1; i <= numFacility; i++) {
////                            if (Math.abs(subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) - 1) <= INT_GAP) {
////                                System.out.println("Customer " + j + " is served by Facility " + i + " with cost of " + servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
//////                                totalCost += servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
////                            }
////                        }
////                    }
//
//                    solveSubModel();
//
//                    nodeUB = 0;
//                    for (int i = 1; i <= numFacility; i++) {
//                        nodeUB += masterSolver.getVariableSol("y_" + i) * openCosts.get(String.valueOf(i));
////                        nodeUB = separator.get("y_" + i) * openCosts.get(String.valueOf(i));
//                    }
//                    for (int i = 1; i <= numFacility; i++) {
//                        for (int j = 1; j <= numCustomer; j++) {
//                            nodeUB += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
//                        }
//                    }
//
//                    if (ub > nodeUB) {
//                        ub = nodeUB;
//                    }
////                    updateUB();
//                }
//                addBendersCutToMaster();
                addBendersCut(separator);
//                addOptimalityCutToMaster();
                masterSolver.solveLP();
//                nodeLB = masterSolver.getOptimum();
                if (Math.abs(masterSolver.getOptimum() - nodeLB) >= 1) {
                    nodeLB = masterSolver.getOptimum();
                    lbNoImprovement = 0;
                } else {
                    nodeLB = masterSolver.getOptimum();
                    lbNoImprovement++;
                }

                if (lbNoImprovement == 5) {
                    LAMDA = 1;
//                    lbNoImprovement = 0;
                }

                if (lbNoImprovement == 10) {
                    DELTA = 0;
//                    lbNoImprovement = 0;
                }

                if (lbNoImprovement == 15) {
                    break;
                }

                if (step % 5 == 0)
                    removeSlackCutOfMaster();
//                    DELTA = 0;
            }
        }

        removeSlackCutOfMaster();
        masterSolver.solveLP();

//
//        if (integerFound) {
//            System.out.println("UB = " + ub + ", time = " + (System.currentTimeMillis() - start));
//            return;
//        }

        BranchingConstraintSet target = new BranchingConstraintSet(0);

        branching(target);

//        int step = 1;
        while (!branchingConsSet.isEmpty()) {
//            if(step % 1000 == 0)
//                System.out.println(ub + "   node size " + branchingConsSet.size() + "   step = " + step);
            target = branchingConsSet.get(0);
            branchingConsSet.remove(0);

            for (BranchingConstraint branching : target.branchingConstraints) {
                Map<String, Double> terms = new HashMap<>();
                terms.put(branching.branchingVarName, 1.0);
                if (branching.branchingType == ConstraintType.LEQL)
                    masterSolver.addConstraint(branching.name, terms, branching.branchingType, -Double.MAX_VALUE, branching.bound);
                else
                    masterSolver.addConstraint(branching.name, terms, branching.branchingType, branching.bound, Double.MAX_VALUE);
            }

            masterSolver.solveLP();

            if (masterSolver.getStatus() == Model.Status.OPTIMAL) {
                nodeLB = masterSolver.getOptimum();
                if (masterSolver.getOptimum() < ub && Math.abs(masterSolver.getOptimum() - ub) > INT_GAP) {
                    LAMDA = 0.2;

                    DELTA = 2 * INT_GAP;

                    lbNoImprovement = 0;

                    for (int i = 1; i <= numFacility; i++) {
                        yy.put("y_" + i, 1.0);
                    }

                    step = 1;

                    while (Math.abs(nodeLB - nodeUB) > INT_GAP) {
                        System.out.println(nodeLB + " , " + nodeUB);
//                        solveSubModel();

                        Map<String, Double> separator = stabilize();
                        solveSubModelWithSeparator(separator);

//                        nodeUB = 0;
//                        for (int i = 1; i <= numFacility; i++) {
////                            nodeUB += masterSolver.getVariableSol("y_" + i) * openCosts.get(String.valueOf(i));
//                            nodeUB = separator.get("y_" + i) * openCosts.get(String.valueOf(i));
//                        }
//                        for (int i = 1; i <= numFacility; i++) {
//                            for (int j = 1; j <= numCustomer; j++) {
//                                nodeUB += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
//                            }
//                        }

                        if (integerSolution()) {
                            integerFound = true;
//                    for (int i = 1; i <= numFacility; i++) {
//                        if (Math.abs(masterSolver.getVariableSol("y_" + i) - 1) <= INT_GAP) {
//                            System.out.println("Facility " + i + " is opening with cost of " + openCosts.get(String.valueOf(i)));
////                            totalCost += openCosts.get(String.valueOf(i));
//                        }
//                    }
//
//                    for (int j = 1; j <= numCustomer; j++) {
//                        for (int i = 1; i <= numFacility; i++) {
//                            if (Math.abs(subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) - 1) <= INT_GAP) {
//                                System.out.println("Customer " + j + " is served by Facility " + i + " with cost of " + servingCosts.get(String.valueOf(i)).get(String.valueOf(j)));
////                                totalCost += servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
//                            }
//                        }
//                    }

                            solveSubModel();

                            nodeUB = 0;
                            for (int i = 1; i <= numFacility; i++) {
                                nodeUB += masterSolver.getVariableSol("y_" + i) * openCosts.get(String.valueOf(i));
//                        nodeUB = separator.get("y_" + i) * openCosts.get(String.valueOf(i));
                            }
                            for (int i = 1; i <= numFacility; i++) {
                                for (int j = 1; j <= numCustomer; j++) {
                                    nodeUB += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                                }
                            }

                            if (ub > nodeUB) {
                                ub = nodeUB;
                            }
//                    updateUB();
                        }
//                        updateUB();
//                        addBendersCutToMaster();
                        addBendersCut(separator);
                        masterSolver.solveLP();
//                        nodeLB = masterSolver.getOptimum();

                        if (Math.abs(masterSolver.getOptimum() - nodeLB) >= 1) {
                            nodeLB = masterSolver.getOptimum();
                            lbNoImprovement = 0;
                        } else {
                            nodeLB = masterSolver.getOptimum();
                            lbNoImprovement++;
                        }

                        if (lbNoImprovement == 5) {
                            LAMDA = 1;
//                    lbNoImprovement = 0;
                        }

                        if (lbNoImprovement == 10) {
                            DELTA = 0;
//                    lbNoImprovement = 0;
                        }

                        if (lbNoImprovement == 15) {
                            break;
                        }

                        step++;


                        if (step % 5 == 0)
                            removeSlackCutOfMaster();
                    }
                }
                branching(target);
            }

            for (BranchingConstraint branching : target.branchingConstraints) {
                masterSolver.removeConstraint(branching.name);
            }
            step++;
        }
        System.out.println("UB = " + ub);
        System.out.println("LB = " + lb);
        System.out.println("Time " + (System.currentTimeMillis() - start) + " , Step " + step);
    }

    void branchAndCut3() {
        initMaster();  //init the relaxed LP
        initSubModel();

        long start = System.currentTimeMillis();

        masterSolver.solveLP();

        double nodeLB = masterSolver.getOptimum();
        double nodeUB = ub;
        boolean integerFound = false;
        int step = 1;


        if (masterSolver.getOptimum() < ub && Math.abs(masterSolver.getOptimum() - ub) > INT_GAP) {
            while (Math.abs(nodeLB - nodeUB) > INT_GAP) {
                if (step % 1 == 0)
                    System.out.println(step + " , " + nodeLB + " , " + nodeUB + " , " + ub);

                if (step % 5 == 0)
                    removeSlackCutOfMaster();
                step++;

//                Map<String, Double> separator = stabilize();
//                solveSubModelWithSeparator(separator);

                solveSubModel();
                nodeUB = 0;
                for (int i = 1; i <= numFacility; i++) {
                    nodeUB += masterSolver.getVariableSol("y_" + i) * openCosts.get(String.valueOf(i));
//                    nodeUB = separator.get("y_" + i) * openCosts.get(String.valueOf(i));
                }
                for (int i = 1; i <= numFacility; i++) {
                    for (int j = 1; j <= numCustomer; j++) {
                        nodeUB += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                    }
                }

//                nodeUB = currentUb;
//                updateUB();
                if (integerSolution()) {
                    integerFound = true;
//

                    if (ub > nodeUB) {
                        ub = nodeUB;
                    }
//                    updateUB();
                }
                addBendersCutToMaster();
//                addBendersCut(separator);
//                addOptimalityCutToMaster();
                masterSolver.solveLP();
                nodeLB = masterSolver.getOptimum();
            }
        }


//        masterSolver.solveLP();


        BranchingConstraintSet target = new BranchingConstraintSet(0);

        branching(target);

//        int step = 1;
        while (!branchingConsSet.isEmpty()) {
//            if(step % 1000 == 0)
//                System.out.println(ub + "   node size " + branchingConsSet.size() + "   step = " + step);
            target = branchingConsSet.get(0);
            branchingConsSet.remove(0);

            for (BranchingConstraint branching : target.branchingConstraints) {
                Map<String, Double> terms = new HashMap<>();
                terms.put(branching.branchingVarName, 1.0);
                if (branching.branchingType == ConstraintType.LEQL)
                    masterSolver.addConstraint(branching.name, terms, branching.branchingType, -Double.MAX_VALUE, branching.bound);
                else
                    masterSolver.addConstraint(branching.name, terms, branching.branchingType, branching.bound, Double.MAX_VALUE);
            }

            masterSolver.solveLP();

            if (masterSolver.getStatus() == Model.Status.OPTIMAL) {
                nodeLB = masterSolver.getOptimum();
                if (masterSolver.getOptimum() < ub && Math.abs(masterSolver.getOptimum() - ub) > INT_GAP) {
                    step = 0;
                    while (Math.abs(nodeLB - nodeUB) > INT_GAP) {
                        System.out.println(nodeLB + " , " + nodeUB);

                        if (step % 5 == 0)
                            removeSlackCutOfMaster();
                        solveSubModel();

                        nodeUB = 0;
                        for (int i = 1; i <= numFacility; i++) {
                            nodeUB += masterSolver.getVariableSol("y_" + i) * openCosts.get(String.valueOf(i));
//                    nodeUB = separator.get("y_" + i) * openCosts.get(String.valueOf(i));
                        }
                        for (int i = 1; i <= numFacility; i++) {
                            for (int j = 1; j <= numCustomer; j++) {
                                nodeUB += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                            }
                        }
//                        updateUB();
                        if (integerSolution()) {
                            integerFound = true;

                            if (ub > nodeUB) {
                                ub = nodeUB;
                            }
//                    updateUB();
                        }
//                        updateUB();
                        addBendersCutToMaster();
                        masterSolver.solveLP();
                        nodeLB = masterSolver.getOptimum();

                    }
                }
                branching(target);
            }

            for (BranchingConstraint branching : target.branchingConstraints) {
                masterSolver.removeConstraint(branching.name);
            }
            step++;
        }
        System.out.println("UB = " + ub);
        System.out.println("LB = " + lb);
        System.out.println("Time " + (System.currentTimeMillis() - start) + " , Step " + step);
    }


    Boolean integerSolution() {
        for (String varName : complicatingVarNames) {
//            System.out.println(varName + " = " + solver.getVars().get(varName).getValue());
            if (Math.abs(masterSolver.getVariableSol(varName) - 0) <= INT_GAP)
                continue;

            if (Math.abs(masterSolver.getVariableSol(varName) - 1) <= INT_GAP)
                continue;

            return false;

//            if (Math.abs(masterSolver.getVariableSol(varName) - (int) masterSolver.getVariableSol(varName)) >= INT_GAP) {
//                return false;
//            }
        }
        return true;
    }

    boolean solveSubModelWithSeparator(Map<String, Double> separator) {
        long time = System.currentTimeMillis();
        for (String subProblem : subSolvers.keySet()) {
            for (String boundingVar : complicatingVarNames) {
                subSolvers.get(subProblem).setConstraintBound("Bounding with " +
                                boundingVar, -Double.MAX_VALUE,
                        separator.get(boundingVar));
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


    Map<String, Double> stabilize() {
        for (String var : complicatingVarNames) {
            double temp = (yy.get(var) + masterSolver.getVariableSol(var)) * 0.5;
            yy.put(var, temp);
        }

        Map<String, Double> separator = new LinkedHashMap<>();
        for (String var : complicatingVarNames) {
            double temp = LAMDA * masterSolver.getVariableSol(var) + (1 - LAMDA) * yy.get(var) + DELTA;
            separator.put(var, temp);
        }
        return separator;
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

    void addOptimalityCutToMaster() {
        double totalSubOptimum = 0;
        for (String subProblem : subSolvers.keySet()) {
            totalSubOptimum += subSolvers.get(subProblem).getOptimum();
        }

        Map<String, Double> cutTerms = new LinkedHashMap<>();
        for (String masterVar : complicatingVarNames) {
            if (Math.abs(masterSolver.getVariableSol(masterVar) - 0) <= INT_GAP) {
                cutTerms.put(masterVar, -totalSubOptimum);
            }
        }

        cutTerms.put("alpha", -1.0);

        masterSolver.addConstraint("Optimality Cut " + masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum);
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
            totalSubOptimum += subSolvers.get(subProblem).getOptimum();
        }

        masterSolver.addConstraint("Benders Cut " + masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
        bendersCuts.put(masterBendersCutId, "Benders Cut " + masterBendersCutId);
        masterBendersCutId++;
    }

    void bendersDecomposition(int nodeId) {
        while (integerSolution()) {
            if (masterSolver.getOptimum() > ub)
                break;
            lb = masterSolver.getOptimum();
            solveSubModel();
            updateUB();
            addBendersCutToMaster();
            if (lb == ub) {
//                System.out.println("Node " + nodeId + " find optimal solution");
//                System.out.println("UB = " + ub);
//                System.out.println("LB = " + lb);
                return;
            }
            masterSolver.solveLP();
        }

    }

//    protected void solve() {
//
//        masterSolver.solveLP();
////        while (integerSolution()) {
////            lb = masterSolver.getOptimum();
////            solveSubModel();
////            updateUB();
////            addBendersCutToMaster();
////            if (lb == ub) {
////                System.out.println("find optimal solution");
////                System.out.println("UB = " + ub);
////                System.out.println("LB = " + lb);
////                return;
////            }
////            masterSolver.solveLP();
////        }
//        bendersDecomposition();
//
//
//        BranchingConstraintSet target = new BranchingConstraintSet();
//        branching(target);
//
//        int step = 1;
//        while (!branchingConsSet.isEmpty()) {
//            System.out.println(lb + "   " + ub + "   node size " + branchingConsSet.size() + "   step = " + step);
//            target = branchingConsSet.get(0);
//            branchingConsSet.remove(0);
//
//            for (BranchingConstraint branching : target.branchingConstraints) {
//                Map<String, Double> terms = new HashMap<>();
//                terms.put(branching.branchingVarName, 1.0);
//                if (branching.branchingType == ConstraintType.LEQL)
//                    masterSolver.addConstraint(branching.name, terms, branching.branchingType, -Double.MAX_VALUE, branching.bound);
//                else
//                    masterSolver.addConstraint(branching.name, terms, branching.branchingType, branching.bound, Double.MAX_VALUE);
//            }
//
//            masterSolver.solveLP();
//
//            if(masterSolver.getStatus() == Model.Status.OPTIMAL){
//                if(masterSolver.getOptimum() < ub){
//                    bendersDecomposition();
//                    branching(target);
//                }
//            }
//
////            if (masterSolver.getStatus() != Model.Status.OPTIMAL) {
////                for (BranchingConstraint branching : target.branchingConstraints) {
////                    masterSolver.removeConstraint(branching.name);
////                }
////                step++;
////                continue;
////            }
////
////            if (masterSolver.getOptimum() > ub) {
////                for (BranchingConstraint branching : target.branchingConstraints) {
////                    masterSolver.removeConstraint(branching.name);
////                }
////                step++;
////                continue;
////            }
////
////            while (integerSolution()) {
////                if(masterSolver.getOptimum() > ub)
////                    break;
////                lb = masterSolver.getOptimum();
////                solveSubModel();
////                updateUB();
////                addBendersCutToMaster();
////                if (lb == ub) {
////                    System.out.println("find optimal solution");
////                    System.out.println("UB = " + ub);
////                    System.out.println("LB = " + lb);
////                    break;
////                }
////                masterSolver.solveLP();
////            }
//////            bendersDecomposition();
////
////            branching(target);
//            for (BranchingConstraint branching : target.branchingConstraints) {
//                masterSolver.removeConstraint(branching.name);
//            }
//            step++;
//        }
//        System.out.println("UB = " + ub);
//        System.out.println("LB = " + lb);
//
//    }

    void addBendersCut(Map<String, Double> separator) {
        double sumOfBoundingMultipliedDual = 0;

        Map<String, Double> cutTerms = new LinkedHashMap<>();

        for (String masterVar : boundingVarSubDuals.keySet()) {
            double masterVarCoeff = 0;
            for (String subProblem : boundingVarSubDuals.get(masterVar).keySet()) {
                masterVarCoeff += boundingVarSubDuals.get(masterVar).get(subProblem);
                sumOfBoundingMultipliedDual += boundingVarSubDuals.get(masterVar).get(subProblem) * separator.get(masterVar);
            }
            cutTerms.put(masterVar, masterVarCoeff);
        }
        cutTerms.put("alpha", -1.0);

        double totalSubOptimum = 0;
        for (String subProblem : subSolvers.keySet()) {
            totalSubOptimum += subSolvers.get(subProblem).getOptimum();
//            if (subSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL)
//                totalSubOptimum += subSolvers.get(subProblem).getOptimum();
//            else
//                totalSubOptimum += feasibleSubSolvers.get(subProblem).getOptimum();
        }

        masterSolver.addConstraint("Benders Cut " + masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
        bendersCuts.put(masterBendersCutId, "Benders Cut " + masterBendersCutId);
//        masterSolver.addCut(masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
        masterBendersCutId++;
    }

    void branching(BranchingConstraintSet set) {
        String targetBranchingVar = null;
        for (String varName : complicatingVarNames) {

            if (Math.abs(masterSolver.getVariableSol(varName) - 0) <= INT_GAP)
                continue;

            if (Math.abs(masterSolver.getVariableSol(varName) - 1) <= INT_GAP)
                continue;

            targetBranchingVar = varName;
            break;
            /*if (Math.abs(masterSolver.getVariableSol(varName) - (int) masterSolver.getVariableSol(varName)) >= INT_GAP) {
                targetBranchingVar = varName;
                break;
            }*/
        }

        if (targetBranchingVar == null) {
//            System.out.println("Cannot find branching variable, terminate");
            return;
        }

        buildBranchingConstraintSet(set, targetBranchingVar, ConstraintType.LEQL);
        buildBranchingConstraintSet(set, targetBranchingVar, ConstraintType.GEQL);

    }

    protected void updateUB() {
        double currentUb = 0;

        for (int i = 1; i <= numFacility; i++) {
            currentUb += masterSolver.getVariableSol("y_" + i) * openCosts.get(String.valueOf(i));

        }

        for (int i = 1; i <= numFacility; i++) {
            for (int j = 1; j <= numCustomer; j++) {
                currentUb += subSolvers.get("Customer " + j).getVariableSol("x_" + i + "_" + j) * servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
            }
        }

        if (currentUb < ub)
            ub = currentUb;
    }

    //if need further branching, return true; Else, return false
//    boolean solveBranchingModel(BranchingConstraintSet targetSet) {
//        for (BranchingConstraint branching : targetSet.branchingConstraints) {
//            Map<String, Double> terms = new HashMap<>();
//            terms.put(branching.branchingVarName, 1.0);
//
//            if (branching.branchingType == ConstraintType.LEQL)
//                masterSolver.addConstraint(branching.name, terms, branching.branchingType, -Double.MAX_VALUE, branching.bound);
//            else
//                masterSolver.addConstraint(branching.name, terms, branching.branchingType, branching.bound, Double.MAX_VALUE);
//        }
//
//        masterSolver.solveLP();
//
//        for (BranchingConstraint branching : targetSet.branchingConstraints) {
//            masterSolver.removeConstraint(branching.name);
//        }
//
//        if (masterSolver.getStatus() == Model.Status.OPTIMAL) {
//            if (integerSolution()) {
////                Map<String, Double> separator = stabilize();
////                solveSubModelWithSeparator(separator);
////                addBendersCut(separator);
//
//                solveSubModel();
//
////            addBendersCut(separator);
//                addBendersCutToMaster();
//                updateUB();
//
////                if (masterSolver.getOptimum() < ub)
////                    ub = masterSolver.getOptimum();
//                return false;
//            } else {
//                if (masterSolver.getOptimum() > lb)
//                    lb = masterSolver.getOptimum();
//                return true;
//            }
//        } else {
//            System.out.println("Current branch is infeasible!");
//            return false;
//        }
//    }


    void buildBranchingConstraintSet(BranchingConstraintSet targetSet, String branchingVar, ConstraintType branchingType) {
//        String nodeName = targetSet.nodeName + "-";
//        if(branchingType == ConstraintType.LEQL)
//            nodeName += 0;
//        else
//            nodeName += 1;

        BranchingConstraintSet branchingSet = new BranchingConstraintSet(nodeNum);
        nodeNum++;

        branchingSet.branchingConstraints.addAll(targetSet.branchingConstraints);
        int bound = (int) masterSolver.getVariableSol(branchingVar);
        if (branchingType == ConstraintType.LEQL) {
            BranchingConstraint leftBranching = new BranchingConstraint(branchingVar + " <= " + bound, branchingVar, ConstraintType.LEQL, bound);
            branchingSet.branchingConstraints.add(leftBranching);
        } else {
            BranchingConstraint rightBranching = new BranchingConstraint(branchingVar + " >= " + (bound + 1), branchingVar, ConstraintType.GEQL, bound + 1);
            branchingSet.branchingConstraints.add(rightBranching);
        }

        branchingConsSet.add(0, branchingSet);

    }
}
