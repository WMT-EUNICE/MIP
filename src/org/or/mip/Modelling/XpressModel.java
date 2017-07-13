package org.or.mip.Modelling;

import com.dashoptimization.*;
import org.or.mip.BenderDecomposition.BendersCut;
import org.or.mip.BranchBound.BranchAndBound;

import java.util.List;
import java.util.Map;

import static org.or.mip.Modelling.SolverUtils.bcl;

/**
 * Created by baohuaw on 2017/7/3.
 */
public class XpressModel implements Model {
    XPRBprob problem;
    //    XPRBbasis basis = null;
    XPRSprob op;


    public XpressModel(String name) {
        problem = bcl.newProb(name);
        problem.setMsgLevel(1);
        XPRS.init();

        op = problem.getXPRSprob();
    }

    @Override
    public void addVariable(String name, VariableType type, double lb, double ub) {
        if (type == VariableType.REAL) {
            problem.newVar(name, XPRB.PL, lb, ub);
        } else if (type == VariableType.INTEGER) {
            problem.newVar(name, XPRB.UI, lb, ub);
        } else if (type == VariableType.BINARY) {
            problem.newVar(name, XPRB.BV, lb, ub);
        }
    }

    @Override
    public void addConstraint(String name, Map<String, Double> terms, ConstraintType type, double lb,
                              double ub) {
        XPRBexpr ctr = new XPRBexpr();
        for (String varName : terms.keySet()) {
            ctr.add(problem.getVarByName(varName).mul(terms.get(varName)));
        }
        if (type == ConstraintType.EQL)
            problem.newCtr(name, ctr.eql(lb));
        else if (type == ConstraintType.GEQL)
            problem.newCtr(name, ctr.gEql(lb));
        else if (type == ConstraintType.LEQL)
            problem.newCtr(name, ctr.lEql(ub));
    }

    @Override
    public void addCut(int id, Map<String, Double> terms, ConstraintType type, double lb, double ub) {

        problem.beginCB(op);
        problem.sync(XPRB.XPRS_SOL);
        XPRBexpr ctr = new XPRBexpr();
        for (String varName : terms.keySet()) {
            ctr.add(problem.getVarByName(varName).mul(terms.get(varName)));
        }
        XPRBcut cut = problem.newCut(ctr.lEql(ub));
//        cut.setID(id);
        XPRBcut cuts[] = new XPRBcut[1];
        cuts[0] = cut;
        problem.addCuts(cuts);

        problem.endCB();
    }

    @Override
    public void solveLP() {
//        problem.setMsgLevel(4);
        problem.lpOptimise();
    }

    @Override
    public void solveByBranchAndBound(List<String> varNames) {
        BranchAndBound bb = new BranchAndBound(this, varNames);
        bb.branchAndBound();
    }

    @Override
    public void solveByBranchAndBound(List<String> varNames, BendersCut cut) {
        BranchAndBound bb = new BranchAndBound(this, varNames);
        bb.branchAndBound();
    }

    @Override
    public void setVariableType(String varName, VariableType type) {
        if (type == VariableType.REAL) {
            problem.getVarByName(varName).setType(XPRB.PL);
        } else if (type == VariableType.INTEGER) {
            problem.getVarByName(varName).setType(XPRB.UI);
        } else if (type == VariableType.BINARY) {
            problem.getVarByName(varName).setType(XPRB.BV);
        }
    }

    @Override
    public void solveMIP() {
//        problem.setMsgLevel(4);
//        problem.mipOptimise();
//        XPRSprob opt = problem.getXPRSprob();
//        opt.setIntControl(XPRS.MAXTIME, 240);
//        opt.setIntControl(XPRS.MIPTHREADS, 64);
//        problem.setCutMode(0);
        problem.mipOptimise();
//        if(basis == null){
//            problem.mipOptimise();
//            basis = problem.saveBasis();
//        }else{
//            problem.loadMat();
//            problem.loadBasis(basis);
//            basis = null;
//            problem.mipOptimise();
//        }

    }

    /* Callback function reporting loaded solution status */
    static class UserSolNotifyCallback implements XPRSuserSolNotifyListener {
        public void XPRSuserSolNotifyEvent(XPRSprob oprob, Object data, String name, int status) {
            System.out.printf("Optimizer loaded solution %s with status=%d\n", name, status);
        }
    }

    @Override
    public void addInitialSolution(Map<String, Double> varValues) {
//        XPRSprob oprob = problem.getXPRSprob();

        XPRBsol sol = problem.newSol();
        for (String var : varValues.keySet()) {
            sol.setVar(problem.getVarByName(var), varValues.get(var));
        }
        problem.addMIPSol(sol, "heurSol");

         /* Request notification of solution status after processing */
        op.addUserSolNotifyListener(new UserSolNotifyCallback());

   /* Parameter settings to make use of loaded solution */
        op.setDblControl(XPRS.HEURSEARCHEFFORT, 2);
        op.setIntControl(XPRS.HEURSEARCHROOTSELECT, 31);
        op.setIntControl(XPRS.HEURSEARCHTREESELECT, 19);
    }

    @Override
    public void removeConstraint(String ctrName) {
        XPRBctr ctr = problem.getCtrByName(ctrName);
        problem.delCtr(ctr);
    }

    @Override
    public double getVariableSol(String varName) {
        return problem.getVarByName(varName).getSol();
    }

    @Override
    public double getOptimum() {
        return problem.getObjVal();
    }

    @Override
    public double getDual(String ctrName) {

        return problem.getCtrByName(ctrName).getDual();
    }

    @Override
    public String getName() {
        return problem.getName();
    }

    @Override
    public Status getStatus() {
        if (problem.getLPStat() == XPRB.LP_OPTIMAL)
            return Status.OPTIMAL;
        else
            return Status.ELSE;
    }

    @Override
    public void setConstraintBound(String ctrName, double lb, double ub) {
        XPRBctr ctr = problem.getCtrByName(ctrName);
        ctr.setRange(lb, ub);
    }

    @Override
    public void setVariableBound(String varName, double lb, double ub) {
        problem.getVarByName(varName).setLB(lb);
        problem.getVarByName(varName).setUB(ub);
    }

    @Override
    public void setSense(Sense sense) {
        if (sense == Sense.MAX)
            problem.setSense(XPRB.MAXIM);
        else if (sense == Sense.MIN)
            problem.setSense(XPRB.MINIM);
    }

    @Override
    public void setObj(Map<String, Double> terms) {
        XPRBexpr obj = new XPRBexpr();
        for (String varName : terms.keySet())
            obj.add(problem.getVarByName(varName).mul(terms.get(varName)));
        problem.setObj(obj);
    }

    @Override
    public double getSlack(String ctrName) {
        return problem.getCtrByName(ctrName).getSlack();
    }

    @Override
    public boolean hasConstraint(String ctrName) {
        if (problem.getCtrByName(ctrName) == null)
            return false;
        return true;
    }
}
