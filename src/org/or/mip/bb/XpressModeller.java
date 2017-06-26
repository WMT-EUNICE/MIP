package org.or.mip.bb;

import com.dashoptimization.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by baohuaw on 6/23/17.
 */
public class XpressModeller {
    public class ConstraintSet{
        List<XPRBctr> constraints = new LinkedList<>();
        XPRBbasis basis;
    }

    List<ConstraintSet> branchingConsSet = new LinkedList<>();

    XPRB bcl = new XPRB();

    XPRBprob problem = bcl.newProb("Example");      /* Create a new problem in BCL */



    XPRBctr obj;

    double lb = 0;
    double ub = Double.MAX_VALUE;

    Map<String, XPRBvar> vars = new HashMap<>();



    public static void main(){


    }

    void buildInitialConstraintSet(){
//        ConstraintSet set = new ConstraintSet();
        XPRBexpr constraint1 = new XPRBexpr();
        constraint1.add(vars.get("x1"));
        constraint1.add(vars.get("x2"));
//        set.constraints.add();
        problem.newCtr(constraint1.lEql(6));


        XPRBexpr constraint2 = new XPRBexpr();
        constraint1.add(vars.get("x1").mul(5));
        constraint1.add(vars.get("x2").mul(9));
//        set.constraints.add();

        problem.newCtr(constraint2.lEql(45));
//        branchingProblemCons.add(set);
    }

    void buildInitalModel(){

        XPRBvar x1 = problem.newVar(0, Double.MAX_VALUE);
        vars.put("x1", x1);
        XPRBvar x2 = problem.newVar(0, Double.MAX_VALUE);
        vars.put("x2", x2);

        XPRBexpr objExpr = new XPRBexpr();
        objExpr.add(x1.mul(-5));
        objExpr.add(x2.mul(-8));
        obj = problem.newCtr(objExpr);
        problem.setObj(obj);

        XPRBexpr constraint1 = new XPRBexpr();
        constraint1.add(vars.get("x1"));
        constraint1.add(vars.get("x2"));
//        set.constraints.add();
        problem.newCtr(constraint1.lEql(6));


        XPRBexpr constraint2 = new XPRBexpr();
        constraint1.add(vars.get("x1").mul(5));
        constraint1.add(vars.get("x2").mul(9));
//        set.constraints.add();

        problem.newCtr(constraint2.lEql(45));


        problem.setSense(XPRB.MINIM);
    }


    Boolean integerSolution(){
        for(String varName : vars.keySet()){
            if(Math.abs(vars.get(varName).getSol() - (int)vars.get(varName).getSol()) >= 0.00001 ){
                return false;
            }
        }
        return true;
    }

    void solveRelaxedModel(){

        problem.lpOptimize("");             /* Solve the LP-problem */
        System.out.println("Objective: " + problem.getObjVal());  /* Get objective value */

        if(integerSolution()) {
            System.out.println("Integer Solution Found! No branching needed...");
            return;
        }

        lb = problem.getObjVal();

        ConstraintSet set = new ConstraintSet();

        set.basis = problem.saveBasis();
        Iterable<XPRBctr> ctrIter = problem.getCtrs();

        ctrIter.forEach(ctr->{
            set.constraints.add(ctr);
        });
        branchingConsSet.add(set);



        ConstraintSet target = branchingConsSet.get(0);
        branchingConsSet.remove(0);

        branching(target);

        while(!branchingConsSet.isEmpty()){
            target = branchingConsSet.get(0);
            branchingConsSet.remove(0);
            branching(target);



        }



    }

    void buildBranchingConstraintSet(ConstraintSet targetSet, XPRBvar targetVar, boolean left){
        ConstraintSet branchingSet = new ConstraintSet();

        branchingSet.constraints.addAll(targetSet.constraints);
        branchingSet.basis = targetSet.basis;
        //add existing constraints

        XPRBexpr branching = new XPRBexpr();
        branching.add(targetVar);
        if(left) {
            branchingSet.constraints.add(problem.newCtr(branching.lEql(((int) targetVar.getSol()))));
        }else{
            branchingSet.constraints.add(problem.newCtr(branching.gEql(((int) targetVar.getSol() + 1))));
        }
        branchingConsSet.add(branchingSet);

    }

    void branching(ConstraintSet set){
        XPRBvar target = null;
        for(String varName : vars.keySet()){
            if(Math.abs(vars.get(varName).getSol() - (int)vars.get(varName).getSol()) >= 0.00001 ){
                target = vars.get(varName);
                break;
            }
        }

        if(target == null){
            System.out.println("Cannot find branching variable, terminate");
            return;
        }

        buildBranchingConstraintSet(set, target, true);
        buildBranchingConstraintSet(set, target, false);

    }
}
