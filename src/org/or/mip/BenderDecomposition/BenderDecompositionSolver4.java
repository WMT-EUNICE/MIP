package org.or.mip.BenderDecomposition;

import org.or.mip.Modelling.*;

import java.util.*;

/**
 * Created by baohuaw on 6/28/17.
 * <p>
 * Chapter 3 Decompistion Techniques in Integer Programming
 * This example is relating to sub problem infeasibility case
 */
public class BenderDecompositionSolver4 {
    Map<String, Model> subSolvers = new HashMap<>();

    Map<String, Model> feasibleSubSolvers = new HashMap<>();
    Model masterSolver = new XpressModel("Master");


    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;

    final double LARGE_POSTIVE = 10000;

    List<String> complicatingVarNames = new ArrayList<>();

    public static void main(String[] args) {
        BenderDecompositionSolver4 solver = new BenderDecompositionSolver4();
        solver.solve();
    }

    void updateUB(){
        ub = 0;
        if (subSolvers.get("Sub 1").getStatus() == Model.Status.OPTIMAL) {
            ub += -2 * subSolvers.get("Sub 1").getVariableSol("y1");
        } else {
            ub += -2 * feasibleSubSolvers.get("Sub 1").getVariableSol("y1");
        }

        if (subSolvers.get("Sub 2").getStatus() == Model.Status.OPTIMAL) {
            ub += -1 * subSolvers.get("Sub 2").getVariableSol("y2");
        } else {
            ub += -1 * feasibleSubSolvers.get("Sub 2").getVariableSol("y2");
        }

        if (subSolvers.get("Sub 3").getStatus() == Model.Status.OPTIMAL) {
            ub += subSolvers.get("Sub 3").getVariableSol("y3");
        } else {
            ub += feasibleSubSolvers.get("Sub 3").getVariableSol("y3");
        }


        ub += 3 * masterSolver.getVariableSol("x1") + (-3) * masterSolver.getVariableSol("x2");
    }

    void solve() {
        initMaster();
        initSubModel();
        initAlwayseFeasibleSubModel();


        solveMasterModel();
        Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();
        solveSubModel(boundingVarSubDuals);

        updateUB();

        addBendersCutToMaster(boundingVarSubDuals);


        while (Math.abs(ub - lb) >= 1) {
            solveMasterModel();
            solveSubModel(boundingVarSubDuals);

//            for (String subProblem : subSolvers.keySet()){
//                for(String complicatingVarName : complicatingVarNames){
//                    subSolvers.get(subProblem).setConstraintBound("Bounding with " +
//                                    complicatingVarName, masterSolver.getVariableSol(complicatingVarName),
//                            masterSolver.getVariableSol(complicatingVarName));
//                }
//
//                subSolvers.get(subProblem).solve();
//                if(subSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL){
//                    System.out.println(subProblem + " objective value: " + subSolvers.get(subProblem).getOptimum());
//                    for(String complicatingVarName : complicatingVarNames){
//                        boundingVarSubDuals.get(complicatingVarName).put(subProblem,
//                                subSolvers.get(subProblem).getDual("Bounding with " + complicatingVarName));
//                    }
//
//                }else{
//                    for(String complicatingVarName : complicatingVarNames){
//                        feasibleSubSolvers.get(subProblem).setConstraintBound("Bounding with " + complicatingVarName,
//                                masterSolver.getVariableSol(complicatingVarName),
//                                masterSolver.getVariableSol(complicatingVarName));
//                    }
//
//                    feasibleSubSolvers.get(subProblem).solve();
//
//                    if(feasibleSubSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL){
//
//                        System.out.println(subProblem + " objective value: " + feasibleSubSolvers.get(subProblem).getOptimum());
//                        for(String complicatingVarName : complicatingVarNames){
//                            boundingVarSubDuals.get(complicatingVarName).put(subProblem, feasibleSubSolvers.get(subProblem).getDual("Bounding with " + complicatingVarName));
//                        }
//
//                    }
//                }
//            }

            updateUB();

            addBendersCutToMaster(boundingVarSubDuals);
        }

        System.out.println("Upper bound = " + ub);
        System.out.println("Lower bound = " + lb);
    }

    void initSubModel() {
        //definition of sub model
        Model sub1 = new XpressModel("Sub 1");
        sub1.addVariable("y1", VariableType.REAL, 0, Double.MAX_VALUE);
        for (String boundingVar : complicatingVarNames) {
            sub1.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
        }
        Map<String, Double> sub1ObjTerms = new LinkedHashMap<>();
        sub1ObjTerms.put("y1", -2.0);
        sub1.setObj(sub1ObjTerms);
        Map<String, Double> sub1Ctr1Terms = new LinkedHashMap<>();
        sub1Ctr1Terms.put("y1", 1.0);
        sub1Ctr1Terms.put("x1", 1.0);
        sub1Ctr1Terms.put("x2", 1.0);
        sub1.addConstraint("Sub 1 Ctr 1", sub1Ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 3);
        for(String boundingVar : complicatingVarNames){
            Map<String, Double> boundingTerms = new LinkedHashMap<>();
            boundingTerms.put(boundingVar, 1.0);

            sub1.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
                    0 ,0 );
        }


        sub1.setSense(Model.Sense.MIN);
        subSolvers.put("Sub 1", sub1);

        Model sub2 = new XpressModel("Sub 2");
        sub2.addVariable("y2", VariableType.REAL, 0, Double.MAX_VALUE);
        for (String boundingVar : complicatingVarNames) {
            sub2.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
        }
        Map<String, Double> sub2ObjTerms = new LinkedHashMap<>();
        sub2ObjTerms.put("y2", -1.0);
        sub2.setObj(sub2ObjTerms);
        Map<String, Double> sub2Ctr1Terms = new LinkedHashMap<>();
        sub2Ctr1Terms.put("y2", 2.0);
        sub2Ctr1Terms.put("x1", 3.0);
        sub2.addConstraint("Sub 2 Ctr 1", sub2Ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 12);

        for(String boundingVar : complicatingVarNames){
            Map<String, Double> boundingTerms = new LinkedHashMap<>();
            boundingTerms.put(boundingVar, 1.0);

            sub2.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
                    0 ,0 );
        }
        sub2.setSense(Model.Sense.MIN);
        subSolvers.put("Sub 2", sub2);

        //definition of Sub Model 3
        Model sub3 = new XpressModel("Sub 3");
        sub3.addVariable("y3", VariableType.REAL, 0, Double.MAX_VALUE);
        for (String boundingVar : complicatingVarNames) {
            sub3.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
        }
        Map<String, Double> sub3ObjTerms = new LinkedHashMap<>();
        sub3ObjTerms.put("y3", 1.0);
        sub3.setObj(sub3ObjTerms);
        Map<String, Double> sub3Ctr1Terms = new LinkedHashMap<>();
        sub3Ctr1Terms.put("y3", 1.0);
        sub3Ctr1Terms.put("x2", -7.0);
        sub3.addConstraint("Sub 3 Ctr 1", sub3Ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, -16);
        for(String boundingVar : complicatingVarNames){
            Map<String, Double> boundingTerms = new LinkedHashMap<>();
            boundingTerms.put(boundingVar, 1.0);

            sub3.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
                    0 ,0 );
        }
        sub3.setSense(Model.Sense.MIN);
        subSolvers.put("Sub 3", sub3);
    }

    void initAlwayseFeasibleSubModel() {
        Model sub1 = new XpressModel("Sub 1");
        sub1.addVariable("y1", VariableType.REAL, 0, Double.MAX_VALUE);
        for (String boundingVar : complicatingVarNames) {
            sub1.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
        }
        sub1.addVariable("v1", VariableType.REAL, 0, Double.MAX_VALUE);
        sub1.addVariable("w", VariableType.REAL, 0, Double.MAX_VALUE);
        Map<String, Double> sub1ObjTerms = new LinkedHashMap<>();
        sub1ObjTerms.put("y1", -2.0);
        sub1ObjTerms.put("v1", LARGE_POSTIVE);
        sub1ObjTerms.put("w", LARGE_POSTIVE);
        sub1.setObj(sub1ObjTerms);
        Map<String, Double> sub1Ctr1Terms = new LinkedHashMap<>();
        sub1Ctr1Terms.put("y1", 1.0);
        sub1Ctr1Terms.put("x1", 1.0);
        sub1Ctr1Terms.put("x2", 1.0);
        sub1Ctr1Terms.put("v1", 1.0);
        sub1Ctr1Terms.put("w", -1.0);

        sub1.addConstraint("Sub 1 Ctr 1", sub1Ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 3);
        for(String boundingVar : complicatingVarNames){
            Map<String, Double> boundingTerms = new LinkedHashMap<>();
            boundingTerms.put(boundingVar, 1.0);

            sub1.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
                    0 ,0 );
        }
        sub1.setSense(Model.Sense.MIN);
        feasibleSubSolvers.put("Sub 1", sub1);


        Model sub2 = new XpressModel("Sub 2");
        sub2.addVariable("y2", VariableType.REAL, 0, Double.MAX_VALUE);
        for (String boundingVar : complicatingVarNames) {
            sub2.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
        }
        sub2.addVariable("v2", VariableType.REAL, 0, Double.MAX_VALUE);
        sub2.addVariable("w", VariableType.REAL, 0, Double.MAX_VALUE);

        Map<String, Double> sub2ObjTerms = new LinkedHashMap<>();
        sub2ObjTerms.put("y2", -1.0);
        sub2ObjTerms.put("v3", LARGE_POSTIVE);
        sub2ObjTerms.put("w", LARGE_POSTIVE);
        sub2.setObj(sub2ObjTerms);
        Map<String, Double> sub2Ctr1Terms = new LinkedHashMap<>();
        sub2Ctr1Terms.put("y2", 2.0);
        sub2Ctr1Terms.put("x1", 3.0);
        sub2Ctr1Terms.put("v2", 1.0);
        sub2Ctr1Terms.put("w", -1.0);
        sub2.addConstraint("Sub 2 Ctr 1", sub2Ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 12);

        for(String boundingVar : complicatingVarNames){
            Map<String, Double> boundingTerms = new LinkedHashMap<>();
            boundingTerms.put(boundingVar, 1.0);

            sub2.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
                    0 ,0 );
        }
        sub2.setSense(Model.Sense.MIN);
        feasibleSubSolvers.put("Sub 2", sub2);


        //definitin of sub model 3 which is always feasible
        Model sub3 = new XpressModel("Sub 3");
        sub3.addVariable("y3", VariableType.REAL, 0, Double.MAX_VALUE);
        for (String boundingVar : complicatingVarNames) {
            sub3.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
        }
        sub3.addVariable("v3", VariableType.REAL, 0, Double.MAX_VALUE);
        sub3.addVariable("w", VariableType.REAL, 0, Double.MAX_VALUE);
        Map<String, Double> sub3ObjTerms = new LinkedHashMap<>();
        sub3ObjTerms.put("y3", 1.0);
        sub3ObjTerms.put("v3", LARGE_POSTIVE);
        sub3ObjTerms.put("w", LARGE_POSTIVE);
        sub3.setObj(sub3ObjTerms);
        Map<String, Double> sub3Ctr1Terms = new LinkedHashMap<>();
        sub3Ctr1Terms.put("y3", 1.0);
        sub3Ctr1Terms.put("x2", -7.0);
        sub3Ctr1Terms.put("v3", 1.0);
        sub3Ctr1Terms.put("w", -1.0);
        sub3.addConstraint("Sub 3 Ctr 1", sub3Ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, -16);
        for(String boundingVar : complicatingVarNames){
            Map<String, Double> boundingTerms = new LinkedHashMap<>();
            boundingTerms.put(boundingVar, 1.0);

            sub3.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL,
                    0 ,0 );
        }
        sub3.setSense(Model.Sense.MIN);
        feasibleSubSolvers.put("Sub 3", sub3);
    }

    void initMaster() {
        //definition of complicating vars
        complicatingVarNames.add("x1");
        complicatingVarNames.add("x2");

        //definition of master model
        masterSolver.addVariable("x1", VariableType.REAL, 0, Double.MAX_VALUE);
        masterSolver.addVariable("x2", VariableType.REAL, 0, Double.MAX_VALUE);
        masterSolver.addVariable("alpha", VariableType.REAL, -100, Double.MAX_VALUE);
        Map<String, Double> objTerms = new LinkedHashMap<>();
        objTerms.put("x1", 3.0);
        objTerms.put("x2", -3.0);
        objTerms.put("alpha", 1.0);
        masterSolver.setObj(objTerms);
        Map<String, Double> ctr1Terms = new LinkedHashMap<>();
        ctr1Terms.put("x1", -1.0);
        ctr1Terms.put("x2", 1.0);
        masterSolver.addConstraint("Master Ctr 1", ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 2);
        masterSolver.setSense(Model.Sense.MIN);
    }

    void solveMasterModel() {
        masterSolver.solveLP();
        lb = masterSolver.getOptimum();
    }

    void solveSubModel(Map<String, Map<String, Double>> boundingVarSubDuals) {
        for (String subProblem : subSolvers.keySet()) {
            for (String boundingVar : complicatingVarNames) {
                subSolvers.get(subProblem).setConstraintBound("Bounding with " +
                                boundingVar, masterSolver.getVariableSol(boundingVar),
                        masterSolver.getVariableSol(boundingVar));
            }

            subSolvers.get(subProblem).solveLP();

            if (subSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL) {
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

                if (feasibleSubSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL) {
                    System.out.println("Sub model objective value: " + feasibleSubSolvers.get(subProblem).getOptimum());
                } else {
                    System.out.println("Bug exists when building the alwayse feasible model");
                }

                for (String boundingVar : complicatingVarNames) {
                    if (!boundingVarSubDuals.containsKey(boundingVar))
                        boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());

                    boundingVarSubDuals.get(boundingVar).put(subProblem, feasibleSubSolvers.get(subProblem).getDual("Bounding with " + boundingVar));
                }
            }
        }
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
            if(subSolvers.get(subProblem).getStatus() == Model.Status.OPTIMAL)
                totalSubOptimum += subSolvers.get(subProblem).getOptimum();
            else
                totalSubOptimum += feasibleSubSolvers.get(subProblem).getOptimum();
        }

        masterSolver.addConstraint("Benders Cut", cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
    }
}
