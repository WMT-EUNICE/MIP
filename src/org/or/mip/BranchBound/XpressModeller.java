package org.or.mip.BranchBound;

import com.dashoptimization.*;

import java.util.*;

/**
 * Created by baohuaw on 6/23/17.
 */
public class XpressModeller {
    enum BoundingType {
        LE, GE
    }


    public class BoundingConstraint {

        String boundingVarName;
        BoundingType type;
        int bound;

        public BoundingConstraint(String boundingVarName, BoundingType type, int bound) {
            this.boundingVarName = boundingVarName;
            this.type = type;
            this.bound = bound;
        }
    }

    public class BranchingConstraintSet {
        List<BoundingConstraint> constraints = new LinkedList<>();
        XPRBbasis basis;
    }

    List<BranchingConstraintSet> branchingConsSet = new LinkedList<>();

    XPRB bcl = new XPRB();

    XPRBprob problem = bcl.newProb("Example");      /* Create a new problem in BCL */


    XPRBctr obj;

    double lb = -Double.MAX_VALUE;
    double ub = Double.MAX_VALUE;

    Map<String, XPRBvar> vars = new HashMap<>();


    public static void main(String[] args) {

        XpressModeller modeller = new XpressModeller();
        modeller.buildInitalModel();
        modeller.branchAndBound();
    }

    void buildInitalModel() {

        XPRBvar x1 = problem.newVar(XPRB.PL, 0, Double.MAX_VALUE);
        vars.put("x1", x1);
        XPRBvar x2 = problem.newVar(XPRB.PL, 0, Double.MAX_VALUE);
        vars.put("x2", x2);

        XPRBexpr objExpr = new XPRBexpr();
        objExpr.add(x1.mul(-5));
        objExpr.add(x2.mul(-8));
        problem.setObj(objExpr);

        XPRBexpr constraint1 = new XPRBexpr();
        constraint1.add(vars.get("x1").mul(1));
        constraint1.add(vars.get("x2").mul(1));
        problem.newCtr("major 1", constraint1.lEql(6));


        XPRBexpr constraint2 = new XPRBexpr();
        constraint2.add(vars.get("x1").mul(5));
        constraint2.add(vars.get("x2").mul(9));
        problem.newCtr("major 2", constraint2.lEql(45));

        problem.setSense(XPRB.MINIM);
    }


    Boolean integerSolution() {
        for (String varName : vars.keySet()) {
            System.out.println(varName + " = " + vars.get(varName).getSol());
            if (Math.abs(vars.get(varName).getSol() - (int) vars.get(varName).getSol()) >= 0.00001) {
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
        problem.lpOptimize("");             /* Solve the LP-problem */
        System.out.println("Objective: " + problem.getObjVal());  /* Get objective value */

        if (integerSolution()) {
            System.out.println("Integer Solution Found! No branching needed...");
            return;
        }

        if (lb < problem.getObjVal())
            lb = problem.getObjVal();


        BranchingConstraintSet target = new BranchingConstraintSet();
        target.basis = problem.saveBasis();
//        branchingConsSet.remove(0);

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
        int branchingId = 0;
        List<XPRBctr> branchingConsName = new ArrayList<>();
         for (BoundingConstraint branching : targetSet.constraints) {
            XPRBexpr expr = new XPRBexpr();
            expr.add(vars.get(branching.boundingVarName));

            XPRBctr b;
            if (branching.type == BoundingType.LE)
                b = problem.newCtr("branching " + branchingId, expr.lEql(branching.bound));
            else {
                b = problem.newCtr("branching " + branchingId, expr.gEql(branching.bound));
            }

            branchingConsName.add(b);
            branchingId++;
        }


//        problem.loadMat();                 /* Reload the problem */
//        problem.loadBasis(targetSet.basis);          /* Load the saved basis */

//        for(Iterator<XPRBctr> iterator = problem.getCtrs().iterator();iterator.hasNext();){
//            XPRBctr ctr = iterator.next();
//            for(String var : vars.keySet()){
//                System.out.println(var + " * " + ctr.getCoefficient(vars.get(var)));
//            }
//            System.out.println(ctr.getRangeL() + " <= expr <= " + ctr.getRangeU());
//        }

        problem.lpOptimize("");             /* Solve the LP-problem */

        for (XPRBctr b : branchingConsName) {
            problem.delCtr(b);
        }

        if (problem.getLPStat() == XPRB.LP_OPTIMAL) {
            System.out.println("Objective: " + problem.getObjVal());  /* Get objective value */
            if (integerSolution()) {
                if (problem.getObjVal() < ub)
                    ub = problem.getObjVal();

//                if (problem.getObjVal() > lb)
//                    lb = problem.getObjVal();

                return false;
            } else {
                if (problem.getObjVal() > lb)
                    lb = problem.getObjVal();
//                else
//                    return false;
            }
        } else {
            System.out.println("Current branch is infeasible!");
            return false;
        }
        return true;
    }

    void buildBranchingConstraintSet(BranchingConstraintSet targetSet, String targetVarName, boolean left) {
        BranchingConstraintSet branchingSet = new BranchingConstraintSet();

        branchingSet.constraints.addAll(targetSet.constraints);
        branchingSet.basis = targetSet.basis;
        //add existing constraints

        XPRBexpr branching = new XPRBexpr();
        branching.add(vars.get(targetVarName));
        int bound = (int) vars.get(targetVarName).getSol();
        if (left) {

            branchingSet.constraints.add(new BoundingConstraint(targetVarName, BoundingType.LE, bound));
        } else {
            branchingSet.constraints.add(new BoundingConstraint(targetVarName, BoundingType.GE, bound + 1));
        }


        branchingConsSet.add(branchingSet);

    }

//    void buildBranchingVarSet(BranchingVarSet targetSet, XPRBvar targetVar, boolean left) {
//        BranchingVarSet branchingSet = new BranchingVarSet();
//
//        branchingSet.branch.putAll(targetSet.branch);
//        branchingSet.basis = targetSet.basis;
//        //add existing constraints
//
//        XPRBexpr branching = new XPRBexpr();
//        branching.add(targetVar);
//        if (left) {
//            branchingSet.branch.put(targetVar.getName(), (int) targetVar.getSol());
//        } else {
//            branchingSet.branch.put(branching.lEql(((int) targetVar.getSol() + 1)));
//        }
//
//
//        branchingConsSet.add(branchingSet);
//
//    }

    void branching(BranchingConstraintSet set) {
        XPRBvar target = null;
        String targetName = null;
        for (String varName : vars.keySet()) {
            if (Math.abs(vars.get(varName).getSol() - (int) vars.get(varName).getSol()) >= 0.00001) {
                target = vars.get(varName);
                targetName = varName;
                break;
            }
        }

        if (target == null) {
            System.out.println("Cannot find branching variable, terminate");
            return;
        }

        buildBranchingConstraintSet(set, targetName, true);
        buildBranchingConstraintSet(set, targetName, false);

    }

}
