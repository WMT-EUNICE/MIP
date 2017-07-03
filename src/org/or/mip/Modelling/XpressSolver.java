package org.or.mip.Modelling;

import com.dashoptimization.XPRB;
import com.dashoptimization.XPRBctr;
import com.dashoptimization.XPRBexpr;
import com.dashoptimization.XPRBprob;

import java.util.HashMap;
import java.util.Map;

import static org.or.mip.Modelling.SolverUtils.bcl;

/**
 * Created by baohuaw on 2017/6/30.
 */
public class XpressSolver implements ModelSolver {
    XPRBprob problem;

    String name;
    Expression obj;
    Map<String, Constraint> constraints = new HashMap<>();
    Map<String,Variable> vars = new HashMap<>();

    Status status;

    Sense sense;
    double optimum;

    public XpressSolver(String name) {
        this.name = name;
        problem = bcl.newProb(name);
    }


    @Override
    public void solve() {

        problem.lpOptimise();
        optimum = problem.getObjVal();

        for(String varName : vars.keySet()){
            vars.get(varName).value = problem.getVarByName(varName).getSol();
        }
    }

    @Override
    public void addConstraint(Constraint constraint) {
        constraints.put(constraint.name, constraint);

        XPRBexpr conExpr = new XPRBexpr();
        for(Term term : constraint.terms){
            conExpr.add(problem.getVarByName(term.var.name).mul(term.coef));
        }
        if(constraint.type == ConstraintType.EQL) {
            problem.newCtr(constraint.name, conExpr.eql(constraint.bound));
        }else if(constraint.type == ConstraintType.LEQL) {
            problem.newCtr(constraint.name, conExpr.lEql(constraint.bound));
        }else if(constraint.type == ConstraintType.GEQL) {
            problem.newCtr(constraint.name, conExpr.gEql(constraint.bound));
        }
    }

    public void translateModel() {
        for(String varName : vars.keySet()){
            if(vars.get(varName).type == VariableType.BINARY){
                problem.newVar(varName, XPRB.BV, vars.get(varName).lb,  vars.get(varName).ub);
            }else if( vars.get(varName).type == VariableType.REAL){
                problem.newVar(varName, XPRB.PL,  vars.get(varName).lb,  vars.get(varName).ub);
            }else if( vars.get(varName).type == VariableType.INTEGER){
                problem.newVar(varName, XPRB.UI,  vars.get(varName).lb,  vars.get(varName).ub);
            }
        }

        XPRBexpr xprbObj = new XPRBexpr();
        for(Term term : obj.terms){
            xprbObj.add(problem.getVarByName(term.var.name).mul(term.coef));
        }
        problem.setObj(xprbObj);

        if(sense == Sense.MAX) {
            problem.setSense(XPRB.MAXIM);
        }else {
            problem.setSense(XPRB.MINIM);
        }

        for(String consName : constraints.keySet()){
            XPRBexpr conExpr = new XPRBexpr();
            for(Term term : constraints.get(consName).terms){
                conExpr.add(problem.getVarByName(term.var.name).mul(term.coef));
            }
            if(constraints.get(consName).type == ConstraintType.EQL) {
                problem.newCtr(consName, conExpr.eql(constraints.get(consName).bound));
            }else if(constraints.get(consName).type == ConstraintType.LEQL) {
                problem.newCtr(consName, conExpr.lEql(constraints.get(consName).bound));
            }else if(constraints.get(consName).type == ConstraintType.GEQL) {
                problem.newCtr(consName, conExpr.gEql(constraints.get(consName).bound));
            }
        }
    }

    @Override
    public void removeConstraint(Constraint constraint) {
        XPRBctr target = problem.getCtrByName(constraint.name);
        problem.delCtr(target);
    }

    @Override
    public double getDual(Constraint constraint) {
        XPRBctr target = problem.getCtrByName(constraint.name);
        return target.getDual();
    }

    @Override
    public Expression getObj() {
        return obj;
    }

    @Override
    public void setObj(Expression obj) {
        this.obj = obj;
    }

    @Override
    public void setSense(Sense sense) {
        this.sense = sense;
    }

    @Override
    public Map<String, Variable> getVars() {
        return vars;
    }

    @Override
    public Map<String, Constraint> getConstraints() {
        return constraints;
    }

    @Override
    public double getOptimum() {
        return optimum;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setConstraintBound(Constraint constraint, double bound) {
        constraint.bound = bound;
        problem.getCtrByName(constraint.name).setRange(bound, bound);
    }
}
