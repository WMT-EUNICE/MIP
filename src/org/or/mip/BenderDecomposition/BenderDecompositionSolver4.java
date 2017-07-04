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
    Model masterSolver = new XpressModel("Master");


    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;

    List<String> complicatingVarNames = new ArrayList<>();
//    Map<String, List<String>> subVarNames = new HashMap<>();

    public static void main(String[] args) {
        BenderDecompositionSolver4 solver = new BenderDecompositionSolver4();
        solver.solve();
    }

    void solve() {
        init();
        Map<String, Double> complicatingVarBoundings = solveMasterModel();
        Map<String, Map<String, Double>> boundingVarSubDuals = new LinkedHashMap<>();
        solveSubModel(complicatingVarBoundings, boundingVarSubDuals);

        ub = -2 * subSolvers.get("Sub 1").getVariableSol("y1") + (-1) * subSolvers.get("Sub 2").getVariableSol("y2") +
                subSolvers.get("Sub 3").getVariableSol("y3") +
                3 * masterSolver.getVariableSol("x1") +
                (-3) * masterSolver.getVariableSol("x2");

        addBendersCutToMaster(boundingVarSubDuals, complicatingVarBoundings);


        while (Math.abs(ub - lb) >= 1) {
            masterSolver.solve();

            lb = masterSolver.getOptimum();

            for (String complicatingVarName : complicatingVarNames) {
                complicatingVarBoundings.put(complicatingVarName, masterSolver.getVariableSol(complicatingVarName));
                for(String subProblem : subSolvers.keySet()){
                    subSolvers.get(subProblem).setConstraintBound("Bounding with " + complicatingVarName, masterSolver.getVariableSol(complicatingVarName),
                            masterSolver.getVariableSol(complicatingVarName));
                    subSolvers.get(subProblem).solve();
                    System.out.println(subProblem + " objective value: " + subSolvers.get(subProblem).getOptimum());
                    boundingVarSubDuals.get(complicatingVarName).put(subProblem, subSolvers.get(subProblem).getDual("Bounding with " + complicatingVarName));
                }
            }

//            for (String complicatingVarName : complicatingVarNames) {
//                boundingVarDuals.put(complicatingVarName, subSolvers.get("Sub_y").getDual("Bounding with " + complicatingVarName));
//            }

//            ub = -subSolvers.get("Sub_y").getVariableSol("x") * 0.25 - subSolvers.get("Sub_y").getVariableSol("y");
            ub = -2 * subSolvers.get("Sub 1").getVariableSol("y1") + (-1) * subSolvers.get("Sub 2").getVariableSol("y2") +
                    subSolvers.get("Sub 3").getVariableSol("y3") +
                    3 * masterSolver.getVariableSol("x1") +
                    (-3) * masterSolver.getVariableSol("x2");

            addBendersCutToMaster(boundingVarSubDuals,  complicatingVarBoundings);
        }

        System.out.println("Upper bound = " + ub);
        System.out.println("Lower bound = " + lb);
    }

    void init() {
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
        masterSolver.setSense(ModelSolver.Sense.MIN);

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
        sub1.setSense(ModelSolver.Sense.MIN);
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
        sub2.setSense(ModelSolver.Sense.MIN);
        subSolvers.put("Sub 2", sub2);


        Model sub3 = new XpressModel("Sub 3");
        sub3.addVariable("y3", VariableType.REAL, 0, Double.MAX_VALUE);
        for (String boundingVar : complicatingVarNames) {
            sub3.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
        }
//        sub3.addVariable("v3", VariableType.REAL, 0, Double.MAX_VALUE);
//        sub3.addVariable("w", VariableType.REAL, 0, Double.MAX_VALUE);
        Map<String, Double> sub3ObjTerms = new LinkedHashMap<>();
        sub3ObjTerms.put("y3", 1.0);
//        sub3ObjTerms.put("v3", 20.0);
//        sub3ObjTerms.put("w", 20.0);
        sub3.setObj(sub3ObjTerms);
        Map<String, Double> sub3Ctr1Terms = new LinkedHashMap<>();
        sub3Ctr1Terms.put("y3", 1.0);
        sub3Ctr1Terms.put("x2", -7.0);
//        sub3Ctr1Terms.put("v3", 1.0);
//        sub3Ctr1Terms.put("w", -1.0);
        sub3.addConstraint("Sub 3 Ctr 1", sub3Ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, -16);
        sub3.setSense(ModelSolver.Sense.MIN);
        subSolvers.put("Sub 3", sub3);
    }

    Map<String, Double> solveMasterModel() {
        masterSolver.solve();
        lb = masterSolver.getOptimum();
        Map<String, Double> complicatingVarBoundings = new HashMap<>();
        for (String complicatingVarName : complicatingVarNames) {
            complicatingVarBoundings.put(complicatingVarName, masterSolver.getVariableSol(complicatingVarName));
        }
        return complicatingVarBoundings;
    }

    void solveSubModel( Map<String, Double> complicatingVarBoundings, Map<String, Map<String, Double>> boundingVarSubDuals) {
        for(String subProblem : subSolvers.keySet()){
            Map<String, String> boundingVarCtrMapping = new HashMap<>();
            for (String boundingVar : complicatingVarBoundings.keySet()) {
                Map<String, Double> boundingTerms = new LinkedHashMap<>();
                boundingTerms.put(boundingVar, 1.0);

                subSolvers.get(subProblem).addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL, complicatingVarBoundings.get(boundingVar), complicatingVarBoundings.get(boundingVar));
                boundingVarCtrMapping.put(boundingVar, "Bounding with " + boundingVar);
            }

            subSolvers.get(subProblem).setSense(ModelSolver.Sense.MIN);
            subSolvers.get(subProblem).solve();

            if(subSolvers.get(subProblem).getStatus() == ModelSolver.Status.OPTIMAL)
                System.out.println("Sub model objective value: " + subSolvers.get(subProblem).getOptimum());
            else{
                subSolvers.get(subProblem).addVariable("v3", VariableType.REAL, 0, Double.MAX_VALUE);
                subSolvers.get(subProblem).addVariable("w", VariableType.REAL, 0, Double.MAX_VALUE);
            }

            for (String boundingVar : complicatingVarBoundings.keySet()){
                if(!boundingVarSubDuals.containsKey(boundingVar))
                    boundingVarSubDuals.put(boundingVar, new LinkedHashMap<>());

                boundingVarSubDuals.get(boundingVar).put(subProblem, subSolvers.get(subProblem).getDual(boundingVarCtrMapping.get(boundingVar)));
            }
        }


//        for (String boundingVar : boundingVarCtrMapping.keySet()) {
//            boundingDuals.put(boundingVar, subSolver.getDual(boundingVarCtrMapping.get(boundingVar)));
//        }


//        ub = -subSolver.getVariableSol("x") * 0.25 - subSolver.getVariableSol("y");

//        return boundingDuals;
    }


    void addBendersCutToMaster(Map<String, Map<String, Double>> masterVarDuals,  Map<String, Double> masterVarBoundings) {
        double sumOfBoundingMultipliedDual = 0;

        Map<String, Double> cutTerms = new LinkedHashMap<>();


        for(String masterVar : masterVarDuals.keySet()){
            double masterVarCoeff = 0;
            for (String subProblem : masterVarDuals.get(masterVar).keySet()) {
                masterVarCoeff += masterVarDuals.get(masterVar).get(subProblem);
                sumOfBoundingMultipliedDual += masterVarDuals.get(masterVar).get(subProblem) * masterVarBoundings.get(masterVar);
            }
            cutTerms.put(masterVar, masterVarCoeff);

        }

        cutTerms.put("alpha", -1.0);

        double totalSubOptimum = 0;
        for(String subProblem : subSolvers.keySet()){
            totalSubOptimum = subSolvers.get(subProblem).getOptimum();
        }

        masterSolver.addConstraint("Benders Cut", cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -totalSubOptimum + sumOfBoundingMultipliedDual);
    }
}
