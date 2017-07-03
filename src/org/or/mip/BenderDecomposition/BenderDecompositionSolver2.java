package org.or.mip.BenderDecomposition;

import org.or.mip.Modelling.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by baohuaw on 6/28/17.
 *
 * Chapter 3 Decompistion Techniques in Integer Programming
 */
public class BenderDecompositionSolver2 {
    Map<String, ModelSolver> subSolvers = new HashMap<>();
    ModelSolver masterSolver = new XpressSolver("Master");


    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;

    public static void main(String[] args) {
        BenderDecompositionSolver2 solver = new BenderDecompositionSolver2();
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

//            master.lpOptimise();
            masterSolver.solve();

            lb = masterSolver.getOptimum();

            for (String complicatingVarName : complicatingVarNames) {
                complicatingVarBoundings.put(complicatingVarName, masterSolver.getVars().get(complicatingVarName).getValue());

//                Constraint boundingCtr = subSolvers.get("Sub_y").getConstraints().get("Bounding with " + complicatingVarName);
//                boundingCtr.setBound(masterSolver.getVars().get(complicatingVarName).getValue());

                subSolvers.get("Sub_y").setConstraintBound(subSolvers.get("Sub_y").getConstraints().get("Bounding with " + complicatingVarName),
                        masterSolver.getVars().get(complicatingVarName).getValue());
//                boundingCtr.setRange(masterVars.get(complicatingVarName).getSol(), masterVars.get(complicatingVarName).getSol());
            }


            subSolvers.get("Sub_y").solve();

            System.out.println("Sub model objective value: " + subSolvers.get("Sub_y").getOptimum());

//            Map<String, Double> boundingDuals = new HashMap<>();

            for (String complicatingVarName : complicatingVarNames) {
                Constraint boundingCtr = subSolvers.get("Sub_y").getConstraints().get("Bounding with " + complicatingVarName);
//                XPRBctr boundingCtr = subSolvers.get("Sub_y").getCtrByName("Bounding with " + complicatingVarName);
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
        Variable x = new Variable("x", VariableType.REAL, 0 ,16);
        masterSolver.getVars().put("x",x);

        Variable alpha = new Variable("alpha", VariableType.REAL, -25, Double.MAX_VALUE);
        masterSolver.getVars().put("alpha", alpha);


        Expression obj = new Expression();

        obj.getTerms().add(new Term(masterSolver.getVars().get("x"), -0.25));
        obj.getTerms().add(new Term(masterSolver.getVars().get("alpha"), 1));
        masterSolver.setObj(obj);

        masterSolver.setSense(ModelSolver.Sense.MIN);

//        master.lpOptimise();
//        masterSolver.setModel(masterSolver.getModel());

        masterSolver.translateModel();
        masterSolver.solve();

        lb = masterSolver.getOptimum();

        Map<String, Double> complicatingVarBoundings = new HashMap<>();

        for (String complicatingVarName : complicatingVarNames) {
            complicatingVarBoundings.put(complicatingVarName, masterSolver.getVars().get(complicatingVarName).getValue());
        }

        return complicatingVarBoundings;
    }

    Map<String, Double> buildSubModel(ModelSolver subSolver, Map<String, Double> complicatingVarBoundings) {

        Variable y = new Variable("y", VariableType.REAL, 0 , Double.MAX_VALUE);
        subSolver.getVars().put("y", y);


        for (String boundingVar : complicatingVarBoundings.keySet()) {
            Variable var = new Variable(boundingVar, VariableType.REAL, 0, Double.MAX_VALUE);
            subSolver.getVars().put(boundingVar, var);
        }

        Expression objExpression = new Expression();
        objExpression.getTerms().add(new Term(subSolver.getVars().get("y"), -1));
        subSolver.setObj(objExpression);

        Constraint constraint1 = new Constraint("Constraint 1", ConstraintType.LEQL,5);
        constraint1.getTerms().add(new Term(subSolver.getVars().get("y"), 1));
        constraint1.getTerms().add(new Term(subSolver.getVars().get("x"), -1));
        subSolver.getConstraints().put(constraint1.getName(), constraint1);

        Constraint constraint2 = new Constraint("Constraint 2", ConstraintType.LEQL,7.5);
        constraint2.getTerms().add(new Term(subSolver.getVars().get("y"), 1));
        constraint2.getTerms().add(new Term(subSolver.getVars().get("x"), -0.5));
        subSolver.getConstraints().put(constraint2.getName(), constraint2);

        Constraint constraint3 = new Constraint("Constraint 3", ConstraintType.LEQL,17.5);
        constraint3.getTerms().add(new Term(subSolver.getVars().get("y"), 1));
        constraint3.getTerms().add(new Term(subSolver.getVars().get("x"), 0.5));
        subSolver.getConstraints().put(constraint3.getName(), constraint3);

        Constraint constraint4 = new Constraint("Constraint 4", ConstraintType.LEQL,10);
        constraint4.getTerms().add(new Term(subSolver.getVars().get("y"), -1));
        constraint4.getTerms().add(new Term(subSolver.getVars().get("x"), 1));
        subSolver.getConstraints().put(constraint4.getName(), constraint4);

        Map<String, Constraint> boundingCtrs = new HashMap<>();
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
