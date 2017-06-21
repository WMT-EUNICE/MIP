package org.or.mip.cg;

import com.dashoptimization.*;
import grph.Grph;
import grph.in_memory.InMemoryGrph;
import org.antlr.v4.runtime.misc.IntSet;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.SingleGraph;
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
    Map<Edge, XPRBctr> edgeConstraints = new HashMap<>();
    Map<Edge, Double> edgeDuals = new HashMap<>();

    //    DefaultDirectedWeightedGraph<Node, CapacitatedEdge> graph;
    Graph graph;
//    DijkstraShortestPath<Node, CapacitatedEdge> pathFinder;
    List<Flow> flows = new ArrayList<>();
    Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "weight");

    public MultiCommodityFlow() {
        buildGraph("src/org/or/mip/cg/tiny_network_01");
//        pathFinder = new DijkstraShortestPath(graph);
        initFlows();
    }

    public static void main(String[] args) {
        MultiCommodityFlow mcf = new MultiCommodityFlow();
        mcf.columnGeneration();
    }

    void columnGeneration() {
        boolean terminate = false;
        while (terminate) {
            solveMaster();
            if (pricing())
                terminate = true;
        }
    }

    void buildGraph(String fileName) {
//        Grph grph = new InMemoryGrph();
//        grph.addVertex(0);
//        graph = new DefaultDirectedWeightedGraph<>(CapacitatedEdge.class);
        graph = new SingleGraph("Network");
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));

            String line = in.readLine();


            Set<String> existingNodeIds = new HashSet<>();
            while (line != null) {
                String[] edgeDescription = line.split("\\s");

                if (!existingNodeIds.contains(edgeDescription[0]))
                    graph.addNode(edgeDescription[0]);
                if (!existingNodeIds.contains(edgeDescription[1]))
                    graph.addNode(edgeDescription[1]);
//                Node from = new Node(edgeDescription[0]);0
//                Node to = new Node(edgeDescription[1]);
//
//
////                if(graph.containsVertex())
//                if(!existingNodeIds.contains(from.label)) {
//                    graph.addVertex(from);
//                    existingNodeIds.add(from.label);
//                }
//                if(!existingNodeIds.contains(to.label)) {
//                    graph.addVertex(to);
//                    existingNodeIds.add(to.label);
//                }

                graph.addEdge(edgeDescription[0] + "-" + edgeDescription[1], edgeDescription[0], edgeDescription[1], true).addAttribute("weight", Double.valueOf(edgeDescription[2]));
                graph.getEdge(edgeDescription[0] + "-" + edgeDescription[1]).addAttribute("capacity", edgeDescription[3]);
                graph.getEdge(edgeDescription[0] + "-" + edgeDescription[1]).addAttribute("originWeight", edgeDescription[2]);
//                CapacitatedEdge edge = graph.addEdge(from, to);
//                graph.setEdgeWeight(edge, Double.valueOf(edgeDescription[2]));
//                edge.capacity = Double.valueOf(edgeDescription[3]);
//                edge.originWeight = graph.getEdgeWeight(edge);
                line = in.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        return graph;
    }

    void initFlows() {
//        Set<Node> nodes = graph.vertexSet();

//        Node from1 = null, from2 = null, to1 = null, to2 = null;
//        for (Node node : nodes) {
//            if (node.label.equals("1")) {
//                from1 = node;
//                from2 = node;
//            } else if (node.label.equals("5"))
//                to1 = node;
//            else if (node.label.equals("8"))
//                to2 = node;
//        }
        flows.add(new Flow(graph.getNode("1"), graph.getNode("5"), 15));
        flows.add(new Flow(graph.getNode("1"), graph.getNode("5"), 10));

        for (Flow f : flows) {
            List<CapacitatedEdge> superPathEdges = new LinkedList<>();

            Path superPath = new Path();
            superPath.getEdgePath().add(graph.getEdge(f.from.label + "-" + f.to.label));

//            for (CapacitatedEdge edge : graph.edgeSet()) {
//                if (graph.getEdgeSource(edge).label.equals(f.from.label) && graph.getEdgeTarget(edge).label.equals(f.to.label) && graph.getEdgeWeight(edge) == 1000) {
//                    superPathEdges.add(edge);
//                }
//            }
//            GraphPath<Node, CapacitatedEdge> superPath = new GraphWalk<>(graph, f.from, f.to, superPathEdges, 1000);
            f.paths.add(superPath);

//            Path sp = Dijkstra.shortestPath(graph, f.from, f.to);
//            f.paths.add(sp);
        }
    }

    void solveMaster() {
        problem.reset();

        XPRS.init();

        Map<Flow, Map<Path, XPRBvar>> x_f_p = new HashMap<>();
        for (Flow f : flows) {
            if (!x_f_p.containsKey(f))
                x_f_p.put(f, new HashMap<>());

            for (Path p : f.paths) {
                XPRBvar var = problem.newVar(XPRB.PL);
                var.setLB(0);
                x_f_p.get(f).put(p, var);
            }
        }

        //obj
        XPRBexpr obj = new XPRBexpr();
        for (Flow f : flows) {

            for (Path p : f.paths) {
//                obj.add(x_f_p.get(f).get(p)).mul(p.getTotalCost());
                obj.addTerm(x_f_p.get(f).get(p), p.getPathWeight("weight"));
            }
        }
        problem.setObj(obj);

        //flow conservation
        for (Flow f : flows) {
            XPRBexpr expr = new XPRBexpr();
            for (Path p : f.paths) {
                expr.addTerm(x_f_p.get(f).get(p), 1);
            }
            flowConstraints.put(f, problem.newCtr(expr.eql(f.volume)));
        }

        //arc capacity
        for (Edge edge : graph.getEdgeSet()) {
            XPRBexpr expr = new XPRBexpr();
            boolean hasFlow = false;
            for (Flow f : flows) {
                for (Path p : f.paths) {
                    if (p.getEdgePath().contains(edge)) {
                        expr.addTerm(x_f_p.get(f).get(p), 1);
                        hasFlow = true;
                    }
                }
            }
            if (hasFlow)
                edgeConstraints.put(edge, problem.newCtr(expr.lEql((double)edge.getAttribute("capacity"))));
        }

        problem.setSense(XPRB.MINIM);

        problem.lpOptimize("");             /* Solve the LP-problem */
        System.out.println("Objective: " + problem.getObjVal());  /* Get objective value */
        for (Flow f : flows) {
            for (Path p : f.paths) {
                System.out.println(f.from + "-" + f.to + ", " + p.toString() + ", " + x_f_p.get(f).get(p).getSol());
            }
        }

        for (Edge e : graph.getEdgeSet()) {
            double flowOnEdge = 0;
            for (Flow flow : flows) {
                for (Path p : flow.paths) {
                    if (p.getEdgePath().contains(e)) {
                        if (Math.abs(x_f_p.get(flow).get(p).getSol()) <= 0.00001)
                            continue;
                        flowOnEdge += x_f_p.get(flow).get(p).getSol();
                    }
                }
            }
//            String[] labels = e.getId().split("-");
            System.out.println("Volume on Edge " + e.toString() + ": " + flowOnEdge + ", capacity: " + e.getAttribute("capacity"));
        }
    }

    boolean pricing() {
        for (Flow f : flowConstraints.keySet()) {
            flowDuals.put(f, flowConstraints.get(f).getDual());
        }

        for (Edge edge : edgeConstraints.keySet()) {
            edgeDuals.put(edge, edgeConstraints.get(edge).getDual());
        }

        for (Edge edge : graph.getEdgeSet()) {
            if (!edgeDuals.containsKey(edge))
                continue;
            edge.setAttribute("originWeight", edge.getAttribute("weight"));
            edge.setAttribute("weight", (double)edge.getAttribute("weight") + edgeDuals.get(edge));
//            graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + edgeDuals.get(edge));
//            edge.setWeight(e.getWeight() + edgeDuals.get(e.getId()));
        }

        boolean noPathFound = true;

        for (Flow f : flows) {
            dijkstra.init(graph);
            dijkstra.setSource(graph.getNode(f.from.label));
            dijkstra.compute();
//            pathFinder.compute();
            Path sp = dijkstra.getPath(graph.getNode(f.to.label));
            if (dijkstra.getPathLength(graph.getNode(f.to.label)) < flowDuals.get(f)) {
                noPathFound = false;
                f.paths.add(sp);
            }
        }

        for (Edge edge : graph.getEdgeSet()) {
            edge.setAttribute("weight", edge.getAttribute("originWeight"));
//            graph.setEdgeWeight(edge, edge.originWeight);
        }
        return noPathFound;
    }

}
