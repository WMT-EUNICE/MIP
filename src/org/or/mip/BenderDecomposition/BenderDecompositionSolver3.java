package org.or.mip.BenderDecomposition;

import org.or.mip.Modelling.*;

import java.util.*;

/**
 * Created by baohuaw on 6/28/17.
 * <p>
 * Chapter 3 Decompistion Techniques in Integer Programming
 */
public class BenderDecompositionSolver3 {
    Map<String, Model> subSolvers = new HashMap<>();
    Model masterSolver = new XpressModel("Master");


    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;

    public static void main(String[] args) {
        BenderDecompositionSolver3 solver = new BenderDecompositionSolver3();
        solver.solve();
    }

    void solve() {
        init();
        List<String> complicatingVarNames = new ArrayList<>();
        complicatingVarNames.add("x");

        Map<String, Double> complicatingVarBoundings = buildMasterModel(complicatingVarNames);

        Map<String, Double> boundingVarDuals = buildSubModel(subSolvers.get("Sub_y"), complicatingVarBoundings);

        addBendersCutToMaster(boundingVarDuals, subSolvers.get("Sub_y"), complicatingVarBoundings);


        while (Math.abs(ub - lb) >= 1) {
            masterSolver.solveLP();

            lb = masterSolver.getOptimum();

            for (String complicatingVarName : complicatingVarNames) {
                complicatingVarBoundings.put(complicatingVarName, masterSolver.getVariableSol(complicatingVarName));
                subSolvers.get("Sub_y").setConstraintBound("Bounding with " + complicatingVarName, masterSolver.getVariableSol(complicatingVarName),
                        masterSolver.getVariableSol(complicatingVarName));
            }

            subSolvers.get("Sub_y").solveLP();

            System.out.println("Sub model objective value: " + subSolvers.get("Sub_y").getOptimum());

            for (String complicatingVarName : complicatingVarNames) {
                boundingVarDuals.put(complicatingVarName, subSolvers.get("Sub_y").getDual("Bounding with " + complicatingVarName));
            }

            ub = -subSolvers.get("Sub_y").getVariableSol("x") * 0.25 - subSolvers.get("Sub_y").getVariableSol("y");

            addBendersCutToMaster(boundingVarDuals, subSolvers.get("Sub_y"), complicatingVarBoundings);
        }

        System.out.println("Upper bound = " + ub);
        System.out.println("Lower bound = " + lb);
    }

    void init() {
        Model sub_y = new XpressModel("Sub_y");
        subSolvers.put("Sub_y", sub_y);
    }

    Map<String, Double> buildMasterModel(List<String> complicatingVarNames) {
        masterSolver.addVariable("x", VariableType.REAL, 0, 16);
        masterSolver.addVariable("alpha", VariableType.REAL, -25, Double.MAX_VALUE);

        Map<String, Double> objTerms = new LinkedHashMap<>();
        objTerms.put("x", -0.25);
        objTerms.put("alpha", 1.0);
        masterSolver.setObj(objTerms);

        masterSolver.setSense(ModelSolver.Sense.MIN);

//        masterSolver.translateModel();
        masterSolver.solveLP();

        lb = masterSolver.getOptimum();

        Map<String, Double> complicatingVarBoundings = new HashMap<>();

        for (String complicatingVarName : complicatingVarNames) {
            complicatingVarBoundings.put(complicatingVarName, masterSolver.getVariableSol(complicatingVarName));
        }

        return complicatingVarBoundings;
    }

    Map<String, Double> buildSubModel(Model subSolver, Map<String, Double> complicatingVarBoundings) {

        subSolver.addVariable("y", VariableType.REAL, 0, Double.MAX_VALUE);


        for (String boundingVar : complicatingVarBoundings.keySet()) {
            subSolver.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
        }

        Map<String, Double> objTerms = new LinkedHashMap<>();
        objTerms.put("y", -1.0);
        subSolver.setObj(objTerms);

        Map<String, Double> ctr1Terms = new LinkedHashMap<>();
        ctr1Terms.put("y", 1.0);
        ctr1Terms.put("x", -1.0);
        subSolver.addConstraint("Constraint 1", ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 5);

        Map<String, Double> ctr2Terms = new LinkedHashMap<>();
        ctr2Terms.put("y", 1.0);
        ctr2Terms.put("x", -0.5);
        subSolver.addConstraint("Constraint 2", ctr2Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 7.5);

        Map<String, Double> ctr3Terms = new LinkedHashMap<>();
        ctr3Terms.put("y", 1.0);
        ctr3Terms.put("x", 0.5);
        subSolver.addConstraint("Constraint 3", ctr3Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 17.5);

        Map<String, Double> ctr4Terms = new LinkedHashMap<>();
        ctr4Terms.put("y", -1.0);
        ctr4Terms.put("x", 1.0);
        subSolver.addConstraint("Constraint 4", ctr4Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 10);

        Map<String, String> boundingVarCtrMapping = new HashMap<>();
        for (String boundingVar : complicatingVarBoundings.keySet()) {
            Map<String, Double> boundingTerms = new LinkedHashMap<>();
            boundingTerms.put(boundingVar, 1.0);

            subSolver.addConstraint("Bounding with " + boundingVar, boundingTerms, ConstraintType.EQL, complicatingVarBoundings.get(boundingVar), complicatingVarBoundings.get(boundingVar));
            boundingVarCtrMapping.put(boundingVar, "Bounding with " + boundingVar);
        }

        subSolver.setSense(ModelSolver.Sense.MIN);
        subSolver.solveLP();

        System.out.println("Sub model objective value: " + subSolver.getOptimum());

        Map<String, Double> boundingDuals = new HashMap<>();
        for (String boundingVar : boundingVarCtrMapping.keySet()) {
            boundingDuals.put(boundingVar, subSolver.getDual(boundingVarCtrMapping.get(boundingVar)));
        }


        ub = -subSolver.getVariableSol("x") * 0.25 - subSolver.getVariableSol("y");

        return boundingDuals;
    }

    void addBendersCutToMaster(Map<String, Double> masterVarDuals, Model subSolver, Map<String, Double> masterVarBoundings) {
        double sumOfBoundingMultipliedDual = 0;

        Map<String, Double> cutTerms = new LinkedHashMap<>();

        for (String masterVar : masterVarDuals.keySet()) {
            cutTerms.put(masterVar, masterVarDuals.get(masterVar));
            sumOfBoundingMultipliedDual += masterVarDuals.get(masterVar) * masterVarBoundings.get(masterVar);
        }
        cutTerms.put("alpha", -1.0);

        masterSolver.addConstraint("Benders Cut", cutTerms, ConstraintType.LEQL, -Double.MAX_VALUE, -subSolver.getOptimum() + sumOfBoundingMultipliedDual);
    }

}
