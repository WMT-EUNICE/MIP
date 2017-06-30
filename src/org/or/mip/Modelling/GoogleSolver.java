package org.or.mip.Modelling;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;

/**
 * Created by baohuaw on 2017/6/30.
 */
public class GoogleSolver implements ModelSolver{
    static {
        System.loadLibrary("jniortools");
    }

    MPSolver solver;

    Model model;

    MPSolver createSolver(String solverType) {
        try {
            return new MPSolver("",
                    MPSolver.OptimizationProblemType.valueOf(solverType));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Model getModel() {
        return null;
    }

    @Override
    public void setModel(Model model) {
        this.model = model;
        solver = createSolver("");
    }

    @Override
    public ModelSolverType getType() {
        return null;
    }

    @Override
    public void solve() {

    }

    @Override
    public void addConstraint(Constraint constraint) {

    }

    @Override
    public void translateModel() {
        for(Variable var : model.vars){
            if(var.type == VariableType.BINARY){
                solver.makeBoolVar(var.name);
            }else if(var.type == VariableType.REAL){
                solver.makeNumVar(var.lb, var.ub, var.name);
            }else if(var.type == VariableType.INTEGER){
                solver.makeIntVar(var.lb, var.ub, var.name);
            }
        }

        MPObjective objective = solver.objective();
        for(Term term : model.obj.terms){
            objective.setCoefficient(solver.lookupVariableOrNull(term.var.name), term.coef);
        }
        if(model.sense == Model.Sense.MIN)
            objective.setMinimization();
        else
            objective.setMaximization();

        for(Constraint cons : model.constraints){
            MPConstraint ct = null;
            if(cons.type == ConstraintType.EQL) {
                ct = solver.makeConstraint(cons.bound, cons.bound);
            }else if(cons.type == ConstraintType.LEQL) {
                ct = solver.makeConstraint(-Double.MAX_VALUE, cons.bound);
            }else if(cons.type == ConstraintType.GEQL) {
                ct = solver.makeConstraint(cons.bound, Double.MAX_VALUE);
            }
            for(Term term : cons.terms){
                ct.setCoefficient(solver.lookupVariableOrNull(term.var.name), term.coef);
            }

        }
    }

    @Override
    public void removeConstraint(Constraint constraint) {
//        solver.lookupConstraintOrNull(constraint.name);
        solver.clear();

        for(Variable var : model.vars){
            if(var.type == VariableType.BINARY){
                solver.makeBoolVar(var.name);
            }else if(var.type == VariableType.REAL){
                solver.makeNumVar(var.lb, var.ub, var.name);
            }else if(var.type == VariableType.INTEGER){
                solver.makeIntVar(var.lb, var.ub, var.name);
            }
        }

        MPObjective objective = solver.objective();
        for(Term term : model.obj.terms){
            objective.setCoefficient(solver.lookupVariableOrNull(term.var.name), term.coef);
        }
        if(model.sense == Model.Sense.MIN)
            objective.setMinimization();
        else
            objective.setMaximization();

        for(Constraint cons : model.constraints){
            if(cons.equals(constraint))
                continue;
            MPConstraint ct = null;
            if(cons.type == ConstraintType.EQL) {
                ct = solver.makeConstraint(cons.bound, cons.bound);
            }else if(cons.type == ConstraintType.LEQL) {
                ct = solver.makeConstraint(-Double.MAX_VALUE, cons.bound);
            }else if(cons.type == ConstraintType.GEQL) {
                ct = solver.makeConstraint(cons.bound, Double.MAX_VALUE);
            }
            for(Term term : cons.terms){
                ct.setCoefficient(solver.lookupVariableOrNull(term.var.name), term.coef);
            }

        }
//        translateModel();
    }
}
