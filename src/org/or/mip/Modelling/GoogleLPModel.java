package org.or.mip.Modelling;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;

import java.util.List;
import java.util.Map;

/**
 * Created by baohuaw on 7/6/17.
 */
public class GoogleLPModel implements Model {
    static {
        System.loadLibrary("jniortools");
    }

    MPSolver solver;
    MPSolver.ResultStatus status;

    public GoogleLPModel(String name) {
        solver = new MPSolver(name, MPSolver.OptimizationProblemType.CLP_LINEAR_PROGRAMMING);
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
    public void addCut(int id, Map<String, Double> terms, ConstraintType type, double lb, double ub) {

    }

    @Override
    public void solveLP() {
        status = solver.solve();
    }

    @Override
    public void solveByBranchAndBound(List<String> varNames) {

    }

    @Override
    public void setVariableType(String varName, VariableType type) {

    }

    @Override
    public void solveMIP() {
        System.out.println("This is a LP solver");
//        status = solver.solve();
    }

    @Override
    public void addInitialSolution(Map<String, Double> varValues) {

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
    public Status getStatus() {
        if(status == MPSolver.ResultStatus.OPTIMAL)
            return Status.OPTIMAL;
        else {
//            System.out.println(status);
            return Status.ELSE;
        }
    }

    @Override
    public void setConstraintBound(String ctrName, double lb, double ub) {
        solver.lookupConstraintOrNull(ctrName).setBounds(lb, ub);
    }

    @Override
    public void setVariableBound(String varName, double lb, double ub) {

    }

    @Override
    public void setSense(Sense sense) {
        if(sense == Sense.MIN)
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

    @Override
    public double getSlack(String ctrName) {
        return 0;
    }

    @Override
    public boolean hasConstraint(String ctrName) {
        return false;
    }

    @Override
    public void printConstraint(String ctrName) {

    }
}
