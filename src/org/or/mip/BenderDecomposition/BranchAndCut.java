package org.or.mip.BenderDecomposition;

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
        List<BranchingConstraint> branchingConstraints = new LinkedList<>();
    }

    double LAMDA = 0.2;

    Map<String, Double> yy = new LinkedHashMap<>();

    final double INT_GAP = 0.00001;

    double DELTA = 2 * INT_GAP;

    Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();

    List<BranchingConstraintSet> branchingConsSet = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        BranchAndCut bc = new BranchAndCut();
//        bc.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/simpleExample2.txt");
        bc.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/GalvaoRaggi/50/50.1");
        bc.branchAndCut();
    }

    void branchAndCut() {
//        for (int i = 1; i <= numFacility; i++) {
//            yy.put("y_" + i, 1.0);
//        }

        initMaster();
        initSubModel();

        solve();


    }


    Boolean integerSolution() {
        for (String varName : complicatingVarNames) {
//            System.out.println(varName + " = " + solver.getVars().get(varName).getValue());
            if (Math.abs(masterSolver.getVariableSol(varName) - (int) masterSolver.getVariableSol(varName)) >= INT_GAP) {
                return false;
            }
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
        masterBendersCutId++;
    }

    protected void solve() {

        masterSolver.solveLP();
//        if(masterSolver.getOptimum() > lb)
//            lb = masterSolver.getOptimum();
        while (integerSolution()) {
            lb = masterSolver.getOptimum();
            solveSubModel();
            updateUB();
            addBendersCutToMaster();
            if (lb == ub) {
                System.out.println("find optimal solution");
                return;
            }
            masterSolver.solveLP();
//            lb = masterSolver.getOptimum();

        }


        BranchingConstraintSet target = new BranchingConstraintSet();
        branching(target);

        int step = 1;
        while (!branchingConsSet.isEmpty()) {
            System.out.println(lb + "   " + ub + "   node size " + branchingConsSet.size() + "   step = " + step);
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

            if (masterSolver.getStatus() != Model.Status.OPTIMAL) {
                for (BranchingConstraint branching : target.branchingConstraints) {
                    masterSolver.removeConstraint(branching.name);
                }
                step++;
                continue;
            }

            if (masterSolver.getOptimum() > ub) {
                for (BranchingConstraint branching : target.branchingConstraints) {
                    masterSolver.removeConstraint(branching.name);
                }
                step++;
                continue;
            }

//            boolean generateNewCut = false;
            while (integerSolution()) {
//                generateNewCut = true;
                if(masterSolver.getOptimum() > ub)
                    break;
                lb = masterSolver.getOptimum();

                solveSubModel();
                updateUB();
                addBendersCutToMaster();
                if (lb == ub) {
                    System.out.println("find optimal solution");
                    System.out.println("UB = " + ub);
                    System.out.println("LB = " + lb);
//                    System.out.println(branchingConsSet.size());
                    break;
                }
                masterSolver.solveLP();


            }

//            if(!generateNewCut)
//                lb = masterSolver.getOptimum();

            branching(target);


            for (BranchingConstraint branching : target.branchingConstraints) {
                masterSolver.removeConstraint(branching.name);
            }
            step++;
        }
        System.out.println("UB = " + ub);
        System.out.println("LB = " + lb);

    }

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
//        bendersCuts.put(masterBendersCutId, "Benders Cut " + masterBendersCutId);
//        masterSolver.addCut(masterBendersCutId, cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
        masterBendersCutId++;
    }

    void branching(BranchingConstraintSet set) {
        String targetBranchingVar = null;
        for (String varName : complicatingVarNames) {
            if (Math.abs(masterSolver.getVariableSol(varName) - (int) masterSolver.getVariableSol(varName)) >= INT_GAP) {
                targetBranchingVar = varName;
                break;
            }
        }

        if (targetBranchingVar == null) {
            System.out.println("Cannot find branching variable, terminate");
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
    boolean solveBranchingModel(BranchingConstraintSet targetSet) {
        for (BranchingConstraint branching : targetSet.branchingConstraints) {
            Map<String, Double> terms = new HashMap<>();
            terms.put(branching.branchingVarName, 1.0);

            if (branching.branchingType == ConstraintType.LEQL)
                masterSolver.addConstraint(branching.name, terms, branching.branchingType, -Double.MAX_VALUE, branching.bound);
            else
                masterSolver.addConstraint(branching.name, terms, branching.branchingType, branching.bound, Double.MAX_VALUE);
        }

        masterSolver.solveLP();

        for (BranchingConstraint branching : targetSet.branchingConstraints) {
            masterSolver.removeConstraint(branching.name);
        }

        if (masterSolver.getStatus() == Model.Status.OPTIMAL) {
            if (integerSolution()) {
//                Map<String, Double> separator = stabilize();
//                solveSubModelWithSeparator(separator);
//                addBendersCut(separator);

                solveSubModel();

//            addBendersCut(separator);
                addBendersCutToMaster();
                updateUB();

//                if (masterSolver.getOptimum() < ub)
//                    ub = masterSolver.getOptimum();
                return false;
            } else {
                if (masterSolver.getOptimum() > lb)
                    lb = masterSolver.getOptimum();
                return true;
            }
        } else {
            System.out.println("Current branch is infeasible!");
            return false;
        }
    }

    void buildBranchingConstraintSet(BranchingConstraintSet targetSet, String branchingVar, ConstraintType branchingType) {
        BranchingConstraintSet branchingSet = new BranchingConstraintSet();

        branchingSet.branchingConstraints.addAll(targetSet.branchingConstraints);
        int bound = (int) masterSolver.getVariableSol(branchingVar);
        if (branchingType == ConstraintType.LEQL) {
            BranchingConstraint leftBranching = new BranchingConstraint(branchingVar + " <= " + bound, branchingVar, ConstraintType.LEQL, bound);
            branchingSet.branchingConstraints.add(leftBranching);
        } else {
            BranchingConstraint rightBranching = new BranchingConstraint(branchingVar + " >= " + (bound + 1), branchingVar, ConstraintType.GEQL, bound + 1);
            branchingSet.branchingConstraints.add(rightBranching);
        }

        branchingConsSet.add(branchingSet);

    }
}
