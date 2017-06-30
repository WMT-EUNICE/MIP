package org.or.mip.BranchBound;

import org.or.mip.Modelling.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by baohuaw on 6/23/17.
 * Mininize cx
 * s.t.
 * Ax <= b
 */
public class BranchAndBoundSolver2 {
    ModelSolver solver = new XpressSolver();
    public class BranchingConstraintSet {
        List<Constraint> constraints = new LinkedList<>();
    }

    List<BranchingConstraintSet> branchingConsSet = new LinkedList<>();

    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;

    Model model = new Model();


    public static void main(String[] args) {

        BranchAndBoundSolver2 modeller = new BranchAndBoundSolver2();

        modeller.buildInitalModel();
        modeller.solver.setModel(modeller.model);
        modeller.branchAndBound();
    }

    void buildInitalModel() {

        Variable x1 = new Variable("x1", VariableType.REAL, 0, Double.MAX_VALUE);
        Variable x2 = new Variable("x2", VariableType.REAL, 0, Double.MAX_VALUE);

        model.getVars().add(x1);
        model.getVars().add(x2);

        Expression obj = new Expression();
        obj.getTerms().add(new Term(x1, -5));
        obj.getTerms().add(new Term(x2, -8));
        model.setObj(obj);

        Constraint constraint1 = new Constraint(ConstraintType.LEQL, 6);
        constraint1.getTerms().add(new Term(x1, 1));
        constraint1.getTerms().add(new Term(x2, 1));
        model.getConstraints().add(constraint1);

        Constraint constraint2 = new Constraint(ConstraintType.LEQL, 45);
        constraint2.getTerms().add(new Term(x1, 5));
        constraint2.getTerms().add(new Term(x2, 9));
        model.getConstraints().add(constraint2);

        model.setSense(Model.Sense.MIN);
    }


    Boolean integerSolution() {
        for (Variable var : model.getVars()) {
            System.out.println(var + " = " + var.getValue());
            if (Math.abs(var.getValue() - (int) var.getValue()) >= 0.00001) {
                return false;
            }
        }
        return true;
    }

    void branchAndBound() {

//        problem.mipOptimize("");             /* Solve the LP-problem */
//        for(String var : vars.keySet()){
//            System.out.println(var + " = " + vars.get(var).getSol());
//        }
//        problem.lpOptimize("");             /* Solve the LP-problem */
        solver.solve();

        System.out.println("Objective: " + model.getOptimum());  /* Get objective value */

        if (integerSolution()) {
            System.out.println("Integer Solution Found! No branching needed...");
            return;
        }

        if (lb < model.getOptimum())
            lb = model.getOptimum();


        BranchingConstraintSet target = new BranchingConstraintSet();

        branching(target);

        while (!branchingConsSet.isEmpty()) {
            if (ub == lb) {
                System.out.println("Terminate because of UB = LB = " + lb);
                return;
            }
            target = branchingConsSet.get(0);
            branchingConsSet.remove(0);

            if (solveBranchingModel(target))
                branching(target);
        }
    }

    //if need further branching, return true; Else, return false
    boolean solveBranchingModel(BranchingConstraintSet targetSet) {
        for (Constraint branching : targetSet.constraints) {

            solver.addConstraint(branching);

        }

        solver.solve();

        for (Constraint branching : targetSet.constraints)  {
            solver.removeConstraint(branching);
        }

        if (model.getStatus() == Model.Status.OPTIMAL) {
            System.out.println("Objective: " + model.getOptimum());  /* Get objective value */
            if (integerSolution()) {
                if (model.getOptimum() < ub)
                    ub = model.getOptimum();
                return false;
            } else {
                if (model.getOptimum() > lb)
                    lb = model.getOptimum();
                return true;
            }
        } else {
            System.out.println("Current branch is infeasible!");
            return false;
        }
    }

    void buildBranchingConstraintSet(BranchingConstraintSet targetSet, Variable branchingVar, boolean left) {
        BranchingConstraintSet branchingSet = new BranchingConstraintSet();

        branchingSet.constraints.addAll(targetSet.constraints);
        int bound = (int) branchingVar.getValue();
        if (left) {

            Constraint leftBranching = new Constraint(ConstraintType.LEQL, bound);
            leftBranching.getTerms().add(new Term(branchingVar, 1));
            branchingSet.constraints.add(leftBranching);
        } else {
            Constraint rightBranching = new Constraint(ConstraintType.GEQL, bound + 1);
            rightBranching.getTerms().add(new Term(branchingVar, 1));
            branchingSet.constraints.add(rightBranching);
        }

        branchingConsSet.add(branchingSet);

    }

    void branching(BranchingConstraintSet set) {
        Variable target = null;
        for (Variable var : model.getVars()) {
            if (Math.abs(var.getValue() - (int) var.getValue()) >= 0.00001) {
                target = var;
                break;
            }
        }

        if (target == null) {
            System.out.println("Cannot find branching variable, terminate");
            return;
        }

        buildBranchingConstraintSet(set, target, true);
        buildBranchingConstraintSet(set, target, false);

    }

}
