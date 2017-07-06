package org.or.mip.Modelling;

import com.dashoptimization.*;

import java.util.Map;

import static org.or.mip.Modelling.SolverUtils.bcl;

/**
 * Created by baohuaw on 2017/7/3.
 */
public class XpressModel implements Model {
    XPRBprob problem;

    public XpressModel(String name) {
        problem = bcl.newProb(name);
        problem.setMsgLevel(4);
    }

    @Override
    public void addVariable(String name, VariableType type, double lb, double ub) {
        if (type == VariableType.REAL) {
            problem.newVar(name, XPRB.PL, lb, ub);
        }else if (type == VariableType.INTEGER) {
            problem.newVar(name, XPRB.UI, lb, ub);
        }else if (type == VariableType.BINARY) {
            problem.newVar(name, XPRB.BV, lb, ub);
        }
    }

    @Override
    public void addConstraint(String name, Map<String, Double> terms, ConstraintType type, double lb,
                              double ub) {
        XPRBexpr ctr = new XPRBexpr();
        for(String varName : terms.keySet()){
            ctr.add(problem.getVarByName(varName).mul(terms.get(varName)));
        }
        if(type == ConstraintType.EQL)
            problem.newCtr(name, ctr.eql(lb));
        else if(type == ConstraintType.GEQL)
            problem.newCtr(name, ctr.gEql(lb));
        else if(type == ConstraintType.LEQL)
            problem.newCtr(name, ctr.lEql(ub));
    }

    @Override
    public void solveLP() {
//        problem.setMsgLevel(4);
        problem.lpOptimise();
    }

    @Override
    public void solveMIP() {
//        problem.setMsgLevel(4);
        problem.mipOptimise();
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
    public ModelSolver.Status getStatus() {
        if(problem.getLPStat() == XPRB.LP_OPTIMAL)
            return ModelSolver.Status.OPTIMAL;
        else
            return ModelSolver.Status.ELSE;
    }

    @Override
    public void setConstraintBound(String ctrName, double lb, double ub) {
        XPRBctr ctr = problem.getCtrByName(ctrName);
        ctr.setRange(lb, ub);
    }

    @Override
    public void setSense(ModelSolver.Sense sense) {
        if(sense == ModelSolver.Sense.MAX)
            problem.setSense(XPRB.MAXIM);
        else if(sense == ModelSolver.Sense.MIN)
            problem.setSense(XPRB.MINIM);
    }

    @Override
    public void setObj(Map<String, Double> terms) {
        XPRBexpr obj = new XPRBexpr();
        for(String varName : terms.keySet())
            obj.add(problem.getVarByName(varName).mul(terms.get(varName)));
        problem.setObj(obj);
    }
}
