package org.or.mip.Modelling;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;

import java.util.Map;

/**
 * Created by baohuaw on 7/6/17.
 */
public class GoogleModel implements Model {
    static {
        System.loadLibrary("jniortools");
    }

    MPSolver solver;
    MPSolver.ResultStatus status;

    public GoogleModel(String name) {
        solver = new MPSolver(name, MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);
    }

    @Override
    public void addVariable(String name, VariableType type, double lb, double ub) {
        if(type == VariableType.REAL){
            solver.makeNumVar(lb, ub, name);
        }else if(type == VariableType.BINARY){
//            if(lb == ub)
            solver.makeBoolVar(name);
        }else if(type == VariableType.INTEGER){
            solver.makeIntVar(lb, ub, name);
        }
    }

    @Override
    public void addConstraint(String name, Map<String, Double> terms, ConstraintType type, double lb, double ub) {
        MPConstraint constraint = solver.makeConstraint(lb, ub, name);
        for(String varName : terms.keySet()){
            constraint.setCoefficient(solver.lookupVariableOrNull(varName), terms.get(varName));
        }
    }

    @Override
    public void solveLP() {
        status = solver.solve();
    }

    @Override
    public void solveMIP() {
        status = solver.solve();
    }

    @Override
    public void removeConstraint(String ctrName) {

    }

    @Override
    public double getVariableSol(String varName) {
        return solver.lookupVariableOrNull(varName).solutionValue();
    }

    @Override
    public double getOptimum() {
        return solver.objective().value();
    }

    @Override
    public double getDual(String ctrName) {
        return solver.lookupConstraintOrNull(ctrName).dualValue();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public ModelSolver.Status getStatus() {
        if(status == MPSolver.ResultStatus.OPTIMAL)
            return ModelSolver.Status.OPTIMAL;
        else {
//            System.out.println(status);
            return ModelSolver.Status.ELSE;
        }
    }

    @Override
    public void setConstraintBound(String ctrName, double lb, double ub) {
        solver.lookupConstraintOrNull(ctrName).setBounds(lb, ub);
    }

    @Override
    public void setSense(ModelSolver.Sense sense) {
        if(sense == ModelSolver.Sense.MIN)
            solver.objective().setMinimization();
        else
            solver.objective().setMaximization();
    }

    @Override
    public void setObj(Map<String, Double> terms) {
        MPObjective objective = solver.objective();
        for(String varName : terms.keySet()){
            objective.setCoefficient(solver.lookupVariableOrNull(varName), terms.get(varName));
        }
//        objective.setMaximization();
    }
}
