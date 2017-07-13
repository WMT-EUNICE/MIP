package org.or.mip.BranchBound;

import org.omg.PortableInterceptor.ORBIdHelper;
import org.or.mip.Modelling.*;

import java.util.*;

/**
 * Created by baohuaw on 6/23/17.
 * Mininize cx
 * s.t.
 * Ax <= b
 */
public class BranchAndBoundSolver2 {
    Model origin = new XpressModel("Branch and Bound");

    public class BranchingConstraint{
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

    List<BranchingConstraintSet> branchingConsSet = new LinkedList<>();

    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;

    final double INT_GAP = 0.00001;
//    Model model = new Model();

    List<String> varNames = new ArrayList<>();

    public static void main(String[] args) {

        BranchAndBoundSolver2 modeller = new BranchAndBoundSolver2();

        modeller.buildInitalModel();
//        modeller.solver.setModel(modeller.model);
        modeller.branchAndBound();
    }

    void buildInitalModel() {

//        Variable x1 = new Variable("x1", VariableType.REAL, 0, Double.MAX_VALUE);
//        Variable x2 = new Variable("x2", VariableType.REAL, 0, Double.MAX_VALUE);
//
//        solver.getVars().put("x1", x1);
//        solver.getVars().put("x2", x2);
        origin.addVariable("x_1", VariableType.REAL, 0, Double.MAX_VALUE);
        origin.addVariable("x_2", VariableType.REAL, 0, Double.MAX_VALUE);
        varNames.add("x_1");
        varNames.add("x_2");

//        Expression obj = new Expression();
//        obj.getTerms().add(new Term(x1, -5));
//        obj.getTerms().add(new Term(x2, -8));
//        solver.setObj(obj);
        Map<String, Double> objTerms = new LinkedHashMap<>();
        objTerms.put("x_1", -5.0);
        objTerms.put("x_2", -8.0);
        origin.setObj(objTerms);


//        Constraint constraint1 = new Constraint("Constraint 1", ConstraintType.LEQL, 6);
//        constraint1.getTerms().add(new Term(x1, 1));
//        constraint1.getTerms().add(new Term(x2, 1));
//        solver.getConstraints().put(constraint1.getName(), constraint1);

        Map<String, Double> ctr1Terms = new LinkedHashMap<>();
        ctr1Terms.put("x_1", 1.0);
        ctr1Terms.put("x_2", 1.0);
        origin.addConstraint("Ctr 1", ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 6);

//        Constraint constraint2 = new Constraint("Constraint 2", ConstraintType.LEQL, 45);
//        constraint2.getTerms().add(new Term(x1, 5));
//        constraint2.getTerms().add(new Term(x2, 9));
//        solver.getConstraints().put(constraint2.getName(), constraint2);

        Map<String, Double> ctr2Terms = new LinkedHashMap<>();
        ctr2Terms.put("x_1", 5.0);
        ctr2Terms.put("x_2", 9.0);
        origin.addConstraint("Ctr 2", ctr1Terms, ConstraintType.LEQL, -Double.MAX_VALUE, 45);

        origin.setSense(Model.Sense.MIN);
    }


    Boolean integerSolution() {
        for (String varName : varNames) {
//            System.out.println(varName + " = " + solver.getVars().get(varName).getValue());
            if (Math.abs(origin.getVariableSol(varName) - (int) origin.getVariableSol(varName)) >= INT_GAP) {
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
        origin.solveLP();

        System.out.println("Objective: " + origin.getOptimum());  /* Get objective value */

        if (integerSolution()) {
            System.out.println("Integer Solution Found! No branching needed...");
            return;
        }

        if (lb < origin.getOptimum())
            lb = origin.getOptimum();


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
        for (BranchingConstraint branching : targetSet.branchingConstraints) {
            Map<String,Double> terms = new HashMap<>();
            terms.put(branching.branchingVarName, 1.0);

            if(branching.branchingType == ConstraintType.LEQL)
                origin.addConstraint(branching.name, terms, branching.branchingType, -Double.MAX_VALUE, branching.bound);
            else
                origin.addConstraint(branching.name, terms, branching.branchingType, branching.bound, Double.MAX_VALUE);

        }

//        solver.solve();
        origin.solveLP();

        for (BranchingConstraint branching : targetSet.branchingConstraints) {
            origin.removeConstraint(branching.name);
        }

        if (origin.getStatus() == Model.Status.OPTIMAL) {
            System.out.println("Objective: " + origin.getOptimum());  /* Get objective value */
            if (integerSolution()) {
                if (origin.getOptimum() < ub)
                    ub = origin.getOptimum();
                return false;
            } else {
                if (origin.getOptimum() > lb)
                    lb = origin.getOptimum();
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
//        int bound = (int) branchingVar.getValue();
        int bound = (int)origin.getVariableSol(branchingVar);
        if (branchingType == ConstraintType.LEQL) {
            BranchingConstraint leftBranching = new BranchingConstraint("Left " + branchingVar, branchingVar, ConstraintType.LEQL, bound);
            branchingSet.branchingConstraints.add(leftBranching);
        } else {
            BranchingConstraint rightBranching = new BranchingConstraint("Right " + branchingVar,branchingVar, ConstraintType.GEQL, bound + 1);
            branchingSet.branchingConstraints.add(rightBranching);
//            Constraint rightBranching = new Constraint("Right", ConstraintType.GEQL, bound + 1);
//            rightBranching.getTerms().add(new Term(branchingVar, 1));
//            branchingSet.constraints.add(rightBranching);
        }

        branchingConsSet.add(branchingSet);

    }

    void branching(BranchingConstraintSet set) {
//        Variable target = null;
        String targetBranchingVar = null;
        for (String varName : varNames) {
            if (Math.abs(origin.getVariableSol(varName) - (int) origin.getVariableSol(varName)) >= 0.00001) {
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

}
