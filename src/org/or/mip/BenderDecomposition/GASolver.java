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
    int TOURNAMENT_ARITY = 2;
    int NUM_GENERATIONS = 1000;
    int POP_SIZE = 200;

    int NUM_FACILITY;
    int NUM_CUSTOMER;

    Map<Integer, Double> openCosts = new LinkedHashMap<>();
    //facility->customer->cost
    Map<Integer, Map<Integer, Double>> servingCosts = new LinkedHashMap<>();

    Map<Integer, Integer> nearestFacilityMappings = new HashMap<>();

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
                    openCosts.put(i, 0.0);
                    if (!servingCosts.containsKey(i)) {
                        servingCosts.put(i, new LinkedHashMap<>());
                    }
                    for (int j = 1; j <= NUM_CUSTOMER; j++) {
                        servingCosts.get(i).put(j, 0.0);
                    }
                }
            } else {
                String[] elements = line.split(" ");
                int facilityId = Integer.valueOf(elements[0]);
                double openCost = Double.valueOf(elements[1]);
                openCosts.put(facilityId, openCost);
                for (int j = 1; j <= elements.length - 2; j++) {
                    servingCosts.get(facilityId).put(j, Double.valueOf(elements[j - 1 + 2]));
                }
            }
            lineId++;
        }
        in.close();
    }

    void initNearestFacilityMapping() {
        for (int j = 1; j <= NUM_CUSTOMER; j++) {
            double minCost = Double.MAX_VALUE;
            int minCostFacilityId = -1;
            for (int i = 1; i <= NUM_FACILITY; i++) {
                if (servingCosts.get(i).get(j) < minCost) {
                    minCost = servingCosts.get(i).get(j);
                    minCostFacilityId = i;
                }
            }
            nearestFacilityMappings.put(j, minCostFacilityId);
        }
    }

    public static void main(String[] args) throws IOException {
        GASolver solver = new GASolver();
        solver.readProblem("/home/local/ANT/baohuaw/IdeaProjects/MIP/data/ufl/KoerkelGhosh-sym/250/a/gs250a-1");
        solver.initNearestFacilityMapping();
        solver.solve();
    }

    public void solve() {
        long start = System.currentTimeMillis();
        // initialize a new genetic algorithm
        GeneticAlgorithm ga = new GeneticAlgorithm(
                new OnePointCrossover<Integer>(),
                1,
                new BinaryMutation(),
                0.10,
                new TournamentSelection(TOURNAMENT_ARITY)
        );

// initial population
        Population initial = randomPopulation(NUM_FACILITY);

        Chromosome bestInitial = initial.getFittestChromosome();

// stopping condition
        StoppingCondition stopCond = new FixedGenerationCount(NUM_GENERATIONS);

// run the algorithm
        Population finalPopulation = ga.evolve(initial, stopCond);

// best chromosome from the final population
        Chromosome bestFinal = finalPopulation.getFittestChromosome();
        System.out.println("Best fitness " + -bestFinal.getFitness());
        System.out.println("Time " + (System.currentTimeMillis() - start));

        LocationChromosome lc = (LocationChromosome)bestFinal;
//        for(int val : ((LocationChromosome) bestFinal).getRepresentation()){
//
//        }
//        for(int val : ((LocationChromosome)bestFinal) )
//        ((FindOnes)bestFinal).
//        ((ElitisticListPopulation) finalPopulation).getChromosomes().get(0).

    }

    /**
     * Initializes a random population.
     */
    private ElitisticListPopulation randomPopulation(int dimension) {
        List<Chromosome> popList = new LinkedList<>();

        for (int i = 0; i < POP_SIZE; i++) {
            BinaryChromosome randChrom = new LocationChromosome(BinaryChromosome.randomBinaryRepresentation(dimension));
            popList.add(randChrom);
        }
        return new ElitisticListPopulation(popList, popList.size(), 0);
    }

    /**
     * Chromosomes represented by a binary chromosome.
     * <p>
     */
    public class LocationChromosome extends BinaryChromosome {

        public LocationChromosome(List<Integer> representation) {
            super(representation);
        }

        /**
         * Returns number of elements != 0
         */
        public double fitness() {
//            int num = 0;
//            for (int val : this.getRepresentation()) {
//                if (val != 0)
//                    num++;
//            }
//            // number of elements >= 0
//            return num;
            double totalCost = 0;
            for (int i = 0; i < this.getRepresentation().size(); i++) {
                if (this.getRepresentation().get(i) == 1) {
                    totalCost += openCosts.get(i + 1);
                }
            }

            for (int j = 1; j <= NUM_CUSTOMER; j++) {
                double minCostWithOpenFacility = Double.MAX_VALUE;
                int minCostFacility = -1;
                for (int i = 0; i < this.getRepresentation().size(); i++) {
                    if (this.getRepresentation().get(i) == 1) {
                        if (servingCosts.get(i + 1).get(j) < minCostWithOpenFacility) {
                            minCostWithOpenFacility = servingCosts.get(i + 1).get(j);
                            minCostFacility = i + 1;
                        }
//                        totalCost += openCosts.get(i + 1);
                    }
                }
                totalCost += minCostWithOpenFacility;
//                totalCost += servingCosts.get(nearestFacilityMappings.get(j)).get(j);
            }


            return -totalCost;
        }


        public List<Integer> getGenes() {
            return this.getRepresentation();
        }

        @Override
        public AbstractListChromosome<Integer> newFixedLengthChromosome(List<Integer> chromosomeRepresentation) {
            return new LocationChromosome(chromosomeRepresentation);
        }

    }
}
