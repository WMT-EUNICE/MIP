package org.or.mip.Modelling;

import com.dashoptimization.XPRB;
import com.dashoptimization.XPRBctr;
import com.dashoptimization.XPRBexpr;
import com.dashoptimization.XPRBprob;

/**
 * Created by baohuaw on 2017/6/30.
 */
public class XpressSolver implements ModelSolver {
    XPRB bcl = new XPRB();

    XPRBprob problem;

    Model model;

//    ModelSolverType type;


    public XpressSolver(){

    }

    public XpressSolver(Model model) {
        this.model = model;
        problem = bcl.newProb(model.name);
    }

    @Override
    public Model getModel() {
        return null;
    }

    @Override
    public void setModel(Model model) {
        this.model = model;
        problem = bcl.newProb(model.name);
    }

    @Override
    public ModelSolverType getType() {
        return null;
    }

    @Override
    public void solve() {

        problem.lpOptimise();
        model.optimum = problem.getObjVal();

        for(Variable var : model.vars){
            var.value = problem.getVarByName(var.name).getSol();
        }
    }

    @Override
    public void addConstraint(Constraint constraint) {
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

    @Override
    public void translateModel() {
        for(Variable var : model.vars){
            if(var.type == VariableType.BINARY){
                problem.newVar(var.name, XPRB.BV, var.lb, var.ub);
            }else if(var.type == VariableType.REAL){
                problem.newVar(var.name, XPRB.PL, var.lb, var.ub);
            }else if(var.type == VariableType.INTEGER){
                problem.newVar(var.name, XPRB.UI, var.lb, var.ub);
            }
        }

        XPRBexpr obj = new XPRBexpr();
        for(Term term : model.obj.terms){
            obj.add(problem.getVarByName(term.var.name).mul(term.coef));
        }
        problem.setObj(obj);

        if(model.sense == Model.Sense.MAX) {
            problem.setSense(XPRB.MAXIM);
        }else {
            problem.setSense(XPRB.MINIM);
        }

        for(Constraint cons : model.constraints){
            XPRBexpr conExpr = new XPRBexpr();
            for(Term term : cons.terms){
                conExpr.add(problem.getVarByName(term.var.name).mul(term.coef));
            }
            if(cons.type == ConstraintType.EQL) {
                problem.newCtr(cons.name, conExpr.eql(cons.bound));
            }else if(cons.type == ConstraintType.LEQL) {
                problem.newCtr(cons.name, conExpr.lEql(cons.bound));
            }else if(cons.type == ConstraintType.GEQL) {
                problem.newCtr(cons.name, conExpr.gEql(cons.bound));
            }
        }
    }

    @Override
    public void removeConstraint(Constraint constraint) {
        XPRBctr target = problem.getCtrByName(constraint.name);
        problem.delCtr(target);
    }
}
