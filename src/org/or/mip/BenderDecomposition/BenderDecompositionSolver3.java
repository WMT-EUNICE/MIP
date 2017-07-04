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
            masterSolver.solve();

            lb = masterSolver.getOptimum();

            for (String complicatingVarName : complicatingVarNames) {
                complicatingVarBoundings.put(complicatingVarName, masterSolver.getVars().get(complicatingVarName).getValue());
                subSolvers.get("Sub_y").setConstraintBound(subSolvers.get("Sub_y").getConstraints().get("Bounding with " + complicatingVarName),
                        masterSolver.getVars().get(complicatingVarName).getValue());
            }

            subSolvers.get("Sub_y").solve();

            System.out.println("Sub model objective value: " + subSolvers.get("Sub_y").getOptimum());

            for (String complicatingVarName : complicatingVarNames) {
                Constraint boundingCtr = subSolvers.get("Sub_y").getConstraints().get("Bounding with " + complicatingVarName);
                boundingVarDuals.put(complicatingVarName, subSolvers.get("Sub_y").getDual(boundingCtr));
            }

            ub = -subSolvers.get("Sub_y").getVars().get("x").getValue() * 0.25 - subSolvers.get("Sub_y").getVars().get("y").getValue();

            addBendersCutToMaster(boundingVarDuals, subSolvers.get("Sub_y"), complicatingVarBoundings);
        }
    }

    void init() {
        ModelSolver sub_y = new XpressSolver("Sub_y");
        subSolvers.put("Sub_y", sub_y);
    }

    Map<String, Double> buildMasterModel(List<String> complicatingVarNames) {
        masterSolver.addVariable("x", VariableType.REAL, 0, 16);
        masterSolver.addVariable("alpha", VariableType.REAL, -25, Double.MAX_VALUE);

        Map<String, Double> objTerms = new LinkedHashMap<>();
        objTerms.put("x", -0.25);
        objTerms.put("alpha", 1);
        masterSolver.setObj(objTerms);

        masterSolver.setSense(ModelSolver.Sense.MIN);

//        masterSolver.translateModel();
        masterSolver.solve();

        lb = masterSolver.getOptimum();

        Map<String, Double> complicatingVarBoundings = new HashMap<>();

        for (String complicatingVarName : complicatingVarNames) {
            complicatingVarBoundings.put(complicatingVarName, masterSolver.getVariableSol(complicatingVarName));
        }

        return complicatingVarBoundings;
    }

    Map<String, Double> buildSubModel(Model subSolver, Map<String, Double> complicatingVarBoundings) {

//        Variable y = new Variable("y", VariableType.REAL, 0, Double.MAX_VALUE);
        subSolver.addVariable("y", VariableType.REAL, 0, Double.MAX_VALUE);


        for (String boundingVar : complicatingVarBoundings.keySet()) {
//            Variable var = new Variable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
            subSolver.addVariable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
//            subSolver.getVars().put(boundingVar, var);
        }

//        Expression objExpression = new Expression();
        Map<String,Double> objTerms = new LinkedHashMap<>();
        objTerms.put("y", -1.0);
        subSolver.setObj(objTerms);

        Map<String,Double> ctr1Terms = new LinkedHashMap<>();
        ctr1Terms.put("y", 1.0);
        ctr1Terms.put("x", -1.0);
        subSolver.addConstraint("Constraint 1", ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 5);

        Map<String,Double> ctr2Terms = new LinkedHashMap<>();
        ctr2Terms.put("y", 1.0);
        ctr2Terms.put("x", -0.5);
        subSolver.addConstraint("Constraint 2", ctr2Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 7.5);

        Map<String,Double> ctr3Terms = new LinkedHashMap<>();
        ctr3Terms.put("y", 1.0);
        ctr3Terms.put("x", 0.5);
        subSolver.addConstraint("Constraint 3", ctr3Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 17.5);

        Map<String,Double> ctr4Terms = new LinkedHashMap<>();
        ctr4Terms.put("y", -1.0);
        ctr4Terms.put("x", 1.0);
        subSolver.addConstraint("Constraint 4", ctr4Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 10);
//        Constraint constraint1 = new Constraint("Constraint 1", ConstraintType.LEQL, 5);
//        constraint1.getTerms().add(new Term(subSolver.getVars().get("y"), 1));
//        constraint1.getTerms().add(new Term(subSolver.getVars().get("x"), -1));
//        subSolver.getConstraints().put(constraint1.getName(), constraint1);

//        Constraint constraint2 = new Constraint("Constraint 2", ConstraintType.LEQL, 7.5);
//        constraint2.getTerms().add(new Term(subSolver.getVars().get("y"), 1));
//        constraint2.getTerms().add(new Term(subSolver.getVars().get("x"), -0.5));
//        subSolver.getConstraints().put(constraint2.getName(), constraint2);

//        Constraint constraint3 = new Constraint("Constraint 3", ConstraintType.LEQL, 17.5);
//        constraint3.getTerms().add(new Term(subSolver.getVars().get("y"), 1));
//        constraint3.getTerms().add(new Term(subSolver.getVars().get("x"), 0.5));
//        subSolver.getConstraints().put(constraint3.getName(), constraint3);

//        Constraint constraint4 = new Constraint("Constraint 4", ConstraintType.LEQL, 10);
//        constraint4.getTerms().add(new Term(subSolver.getVars().get("y"), -1));
//        constraint4.getTerms().add(new Term(subSolver.getVars().get("x"), 1));
//        subSolver.getConstraints().put(constraint4.getName(), constraint4);

//        Map<String, Constraint> boundingCtrs = new HashMap<>();
        List<String> boundingCtrNames = new ArrayList<>();
        for (String boundingVar : complicatingVarBoundings.keySet()) {
            Constraint complicatingVarBoundingCons = new Constraint("Bounding with " + boundingVar, ConstraintType.EQL, complicatingVarBoundings.get(boundingVar));
            complicatingVarBoundingCons.getTerms().add(new Term(subSolver.getVars().get(boundingVar), 1));
            boundingCtrs.put(boundingVar, complicatingVarBoundingCons);
            subSolver.getConstraints().put(complicatingVarBoundingCons.getName(), complicatingVarBoundingCons);
        }

        subSolver.setSense(ModelSolver.Sense.MIN);
        subSolver.translateModel();
        subSolver.solve();

//        ub = sub.getObjVal();
        System.out.println("Sub model objective value: " + subSolver.getOptimum());

        Map<String, Double> boundingDuals = new HashMap<>();
        for (String boundingVar : boundingCtrs.keySet()) {
            boundingDuals.put(boundingVar, subSolver.getDual(boundingCtrs.get(boundingVar)));
        }


        ub = -subSolver.getVars().get("x").getValue() * 0.25 - subSolver.getVars().get("y").getValue();

        return boundingDuals;
    }

    void addBendersCutToMaster(Map<String, Double> masterVarDuals, ModelSolver subSolver, Map<String, Double> masterVarBoundings) {
        Constraint cut = new Constraint("Benders cut", ConstraintType.LEQL, 0);

        double sumOfBoundingMultipliedDual = 0;


        for (String masterVar : masterVarDuals.keySet()) {
            cut.getTerms().add(new Term(masterSolver.getVars().get(masterVar), masterVarDuals.get(masterVar)));
            sumOfBoundingMultipliedDual += masterVarDuals.get(masterVar) * masterVarBoundings.get(masterVar);
        }
        cut.getTerms().add(new Term(masterSolver.getVars().get("alpha"), -1));

        cut.setBound(-subSolver.getOptimum() + sumOfBoundingMultipliedDual);

        masterSolver.addConstraint(cut);
    }

}
