package org.or.mip.BenderDecomposition;

import com.sun.org.apache.bcel.internal.generic.POP;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.genetics.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by baohuaw on 7/10/17.
 */
public class GASolver {
    int TOURNAMENT_ARITY = 10;
    int NUM_GENERATIONS = 1000;
    int POP_SIZE = 100;

    int NUM_FACILITY;
    int NUM_CUSTOMER;

    Map<String, Double> openCosts = new LinkedHashMap<>();
    //facility->customer->cost
    Map<String, Map<String, Double>> servingCosts = new LinkedHashMap<>();

    Map<String, String> nearestFacilityMappings = new HashMap<>();

    void readProblem(String fileName) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String line;
        int lineId = 0;
        while ((line = in.readLine()) != null) {
//            System.out.println(line);
            if (lineId == 0) {
                System.out.println(line);
            } else if (lineId == 1) {
                String[] elements = line.split(" ");
                NUM_FACILITY = Integer.valueOf(elements[0]);
                NUM_CUSTOMER = Integer.valueOf(elements[1]);
                for (int i = 1; i <= NUM_FACILITY; i++) {
                    openCosts.put(String.valueOf(i), 0.0);
                    if (!servingCosts.containsKey(String.valueOf(i))) {
                        servingCosts.put(String.valueOf(i), new LinkedHashMap<>());
                    }
                    for (int j = 1; j <= NUM_CUSTOMER; j++) {
                        servingCosts.get(String.valueOf(i)).put(String.valueOf(j), 0.0);
                    }
                }
            } else {
                String[] elements = line.split(" ");
                String facilityId = elements[0];
                double openCost = Double.valueOf(elements[1]);
                openCosts.put(facilityId, openCost);
                for (int j = 1; j <= elements.length - 2; j++) {
                    servingCosts.get(facilityId).put(String.valueOf(j), Double.valueOf(elements[j - 1 + 2]));
                }
            }
            lineId++;
        }
        in.close();
    }

    void initNearestFacilityMapping(){
        for(int j = 1;j <= NUM_CUSTOMER;j++){
            double minCost = Double.MAX_VALUE;
            String minCostFacility = null;
            for(int i = 1;i <= NUM_FACILITY;i++){
                if(servingCosts.get(String.valueOf(i)).get(String.valueOf(j)) < minCost){
                    minCost = servingCosts.get(String.valueOf(i)).get(String.valueOf(j));
                    minCostFacility = String.valueOf(i);
                }
            }
        }
    }

    Population getInitialPopulation() {

        List<Chromosome> chroms = new LinkedList<>();
        for (int i = 0; i < POP_SIZE; i++) {
            Integer[] repre = new Integer[NUM_FACILITY];
            for (int j = 0; j < NUM_FACILITY; j++) {
                double r = Math.random();
                if (r <= 0.5) {
                    repre[j] = 0;
                }else{
                    repre[j] = 1;
                }
            }
//            chroms.add(new DummyBinaryChromosome(repre));
            Chromosome c1 = new DummyBinaryChromosome(repre) {
                @Override
                public double fitness() {
                    double totalCost = 0;
                    for(int i = 0;i < repre.length;i++){
                        if(repre[i] == 1){
                            totalCost += repre[i] * openCosts.get(String.valueOf(i + 1));
                        }
                    }

                    for(int j = 0;j <= NUM_CUSTOMER;j++){

                    }

                    return totalCost;
                }
            };
        }

        Population pop = new ListPopulation(chroms,chroms.size() ) {
            @Override
            public Population nextGeneration() {
                return null;
            }
        };

        return pop;
    }

    void solve() {
        // initialize a new genetic algorithm
        GeneticAlgorithm ga = new GeneticAlgorithm(
                new OnePointCrossover<Integer>(),
                1,
                new RandomKeyMutation(),
                0.10,
                new TournamentSelection(TOURNAMENT_ARITY)
        );

// initial population
        Population initial = getInitialPopulation();

// stopping condition
        StoppingCondition stopCond = new FixedGenerationCount(NUM_GENERATIONS);

// run the algorithm
        Population finalPopulation = ga.evolve(initial, stopCond);

// best chromosome from the final population
        Chromosome bestFinal = finalPopulation.getFittestChromosome();
    }
}
