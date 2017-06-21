package org.or.mip.cg;

import com.dashoptimization.*;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.GraphWalk;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * Created by baohuaw on 6/21/17.
 */
public class MultiCommodityFlow {
    XPRB bcl = new XPRB();
    XPRBprob problem = bcl.newProb("MCF");      /* Create a new problem in BCL */

    Map<Flow, XPRBctr> flowConstraints = new HashMap<>();
    Map<Flow, Double> flowDuals = new HashMap<>();
    Map<CapacitatedEdge, XPRBctr> edgeConstraints = new HashMap<>();
    Map<CapacitatedEdge, Double> edgeDuals = new HashMap<>();

    DefaultDirectedWeightedGraph<Node, CapacitatedEdge> graph;
    DijkstraShortestPath<Node, CapacitatedEdge> pathFinder = new DijkstraShortestPath(graph);
    List<Flow> flows = new ArrayList<>();

    public MultiCommodityFlow() {
        buildGraph("org/or/mip/cg/tiny_network_01");
        initFlows();
    }

    public static void main(String[] args){
        MultiCommodityFlow mcf = new MultiCommodityFlow();
        mcf.columnGeneration();
    }

    void columnGeneration(){
        boolean terminate = false;
        while (terminate){
            solveMaster();
            if(pricing())
                terminate = true;
        }
    }

    void buildGraph(String fileName) {
        graph = new DefaultDirectedWeightedGraph<>(CapacitatedEdge.class);
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));

            String line = in.readLine();

            while (line != null) {
                String[] edgeDescription = line.split("\\s");
                Node from = new Node(edgeDescription[0]);
                Node to = new Node(edgeDescription[1]);
                graph.addVertex(from);
                graph.addVertex(to);
                CapacitatedEdge edge = graph.addEdge(from, to);
                graph.setEdgeWeight(edge, Double.valueOf(edgeDescription[2]));
                edge.capacity = Double.valueOf(edgeDescription[3]);
                edge.originWeight = graph.getEdgeWeight(edge);
                line = in.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        return graph;
    }

    void initFlows() {
        Set<Node> nodes = graph.vertexSet();

        Node from1 = null, from2 = null, to1 = null, to2 = null;
        for (Node node : nodes) {
            if (node.label.equals("1")) {
                from1 = node;
                from2 = node;
            } else if (node.label.equals("5"))
                to1 = node;
            else if (node.label.equals("8"))
                to2 = node;
        }
        flows.add(new Flow(from1, to1, 15));
        flows.add(new Flow(from2, to2, 10));

        for (Flow f : flows) {
            List<CapacitatedEdge> superPathEdges = new LinkedList<>();

            for (CapacitatedEdge edge : graph.edgeSet()) {
                if (graph.getEdgeSource(edge).equals(f.from) && graph.getEdgeTarget(edge).equals(f.to) && graph.getEdgeWeight(edge) == 1000) {
                    superPathEdges.add(edge);
                }
            }
            GraphPath<Node, CapacitatedEdge> superPath = new GraphWalk<>(graph,f.from,f.to, superPathEdges, 1000);
            f.paths.add(superPath);

//            Path sp = Dijkstra.shortestPath(graph, f.from, f.to);
//            f.paths.add(sp);
        }
    }

    void solveMaster() {
        problem.reset();

        XPRS.init();

        Map<Flow, Map<GraphPath, XPRBvar>> x_f_p = new HashMap<>();
        for (Flow f : flows) {
            if (!x_f_p.containsKey(f))
                x_f_p.put(f, new HashMap<>());

            for (GraphPath p : f.paths) {
                XPRBvar var = problem.newVar(XPRB.PL);
                var.setLB(0);
                x_f_p.get(f).put(p, var);
            }
        }

        //obj
        XPRBexpr obj = new XPRBexpr();
        for (Flow f : flows) {

            for (GraphPath p : f.paths) {
//                obj.add(x_f_p.get(f).get(p)).mul(p.getTotalCost());
                obj.addTerm(x_f_p.get(f).get(p), p.getWeight());
            }
        }
        problem.setObj(obj);

        //flow conservation
        for (Flow f : flows) {
            XPRBexpr expr = new XPRBexpr();
            for (GraphPath p : f.paths) {
                expr.addTerm(x_f_p.get(f).get(p), 1);
            }
            flowConstraints.put(f, problem.newCtr(expr.eql(f.volume)));
        }

        //arc capacity
        for (CapacitatedEdge edge : graph.edgeSet()) {
            XPRBexpr expr = new XPRBexpr();
            boolean hasFlow = false;
            for (Flow f : flows) {
                for (GraphPath p : f.paths) {
                    if(p.getEdgeList().contains(edge)) {
                        expr.addTerm(x_f_p.get(f).get(p), 1);
                        hasFlow = true;
                    }
                }
            }
            if (hasFlow)
                edgeConstraints.put(edge, problem.newCtr(expr.lEql(edge.capacity)));
        }

        problem.setSense(XPRB.MINIM);

        problem.lpOptimize("");             /* Solve the LP-problem */
        System.out.println("Objective: " + problem.getObjVal());  /* Get objective value */
        for (Flow f : flows) {
            for (GraphPath p : f.paths) {
                System.out.println(f.from + "-" + f.to + ", " + p.toString() + ", " + x_f_p.get(f).get(p).getSol());
            }
        }

        for (CapacitatedEdge e : graph.edgeSet()) {
            double flowOnEdge = 0;
            for (Flow flow : flows) {
                for (GraphPath p : flow.paths) {
                    if (p.getEdgeList().contains(e)) {
                        if (Math.abs(x_f_p.get(flow).get(p).getSol()) <= 0.00001)
                            continue;
                        flowOnEdge += x_f_p.get(flow).get(p).getSol();
                    }
                }
            }
//            String[] labels = e.getId().split("-");
            System.out.println("Volume on Edge " + e.toString() + ": " + flowOnEdge + ", capacity: " + e.capacity);
        }
    }

    boolean pricing() {
        for (Flow f : flowConstraints.keySet()) {
            flowDuals.put(f, flowConstraints.get(f).getDual());
        }

        for (CapacitatedEdge edge : edgeConstraints.keySet()) {
            edgeDuals.put(edge, edgeConstraints.get(edge).getDual());
        }

        for (CapacitatedEdge edge : graph.edgeSet()) {
            if (!edgeDuals.containsKey(edge))
                continue;
            edge.originWeight = graph.getEdgeWeight(edge);
            graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + edgeDuals.get(edge));
//            edge.setWeight(e.getWeight() + edgeDuals.get(e.getId()));
        }

        boolean noPathFound = true;

        for (Flow f : flows) {
            GraphPath sp = pathFinder.getPath(f.from, f.to);
            if (sp.getLength() < flowDuals.get(f)) {

                noPathFound = false;
                f.paths.add(sp);
            }
        }

        for (CapacitatedEdge edge : graph.edgeSet()) {
            graph.setEdgeWeight(edge, edge.originWeight);
        }
        return noPathFound;
    }

}
