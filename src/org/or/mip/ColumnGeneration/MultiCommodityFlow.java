package org.or.mip.ColumnGeneration;

import com.dashoptimization.*;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * Created by baohuaw on 6/21/17.
 */
public class MultiCommodityFlow {
    public class Flow {
        Node from;
        Node to;
        double volume;
        List<Path> paths;

        public Flow(Node from, Node to, double volume) {
            this.from = from;
            this.to = to;
            this.volume = volume;
            paths = new LinkedList<>();
        }
    }

    XPRB bcl = new XPRB();

    Map<Flow, XPRBctr> flowConstraints = new HashMap<>();
    Map<Flow, Double> flowDuals = new HashMap<>();
    Map<Edge, XPRBctr> edgeConstraints = new HashMap<>();
    Map<Edge, Double> edgeDuals = new HashMap<>();
    Map<Edge, Double> originWeightOfModifiedEdges = new HashMap<>();

    //    DefaultDirectedWeightedGraph<Node, CapacitatedEdge> graph;
    Graph graph;
    //    DijkstraShortestPath<Node, CapacitatedEdge> pathFinder;
    List<Flow> flows = new ArrayList<>();
    Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "weight");

    public MultiCommodityFlow() {
        buildGraph("src/org/or/mip/cg/tiny_network_01");
        dijkstra.init(graph);
//        pathFinder = new DijkstraShortestPath(graph);
        initFlows();
    }

    public static void main(String[] args) {
        MultiCommodityFlow mcf = new MultiCommodityFlow();
        mcf.columnGeneration();
    }

    void columnGeneration() {
//        boolean genNewCol = true;
        solveMaster();
        while (!pricing()) {
            solveMaster();
//            if (pricing())
//                genNewCol = false;
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

                if (!existingNodeIds.contains(edgeDescription[0])) {
                    graph.addNode(edgeDescription[0]);
                    existingNodeIds.add(edgeDescription[0]);
                }
                if (!existingNodeIds.contains(edgeDescription[1])) {
                    graph.addNode(edgeDescription[1]);
                    existingNodeIds.add(edgeDescription[1]);
                }

                graph.addEdge(edgeDescription[0] + "-" + edgeDescription[1], edgeDescription[0], edgeDescription[1], true).addAttribute("weight", Double.valueOf(edgeDescription[2]));
                graph.getEdge(edgeDescription[0] + "-" + edgeDescription[1]).addAttribute("capacity", Double.valueOf(edgeDescription[3]));
                line = in.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void initFlows() {
        flows.add(new Flow(graph.getNode("1"), graph.getNode("5"), 15));
        flows.add(new Flow(graph.getNode("1"), graph.getNode("8"), 10));

        for (Flow f : flows) {
            Path superPath = new Path();
            superPath.getEdgePath().add(graph.getEdge(f.from.getId() + "-" + f.to.getId()));
            f.paths.add(superPath);
            //Add shortest path of each flow initially
//            dijkstra.setSource(graph.getNode(f.from.getId()));
//            dijkstra.compute();
//            f.paths.add(dijkstra.getPath(f.to));
        }
    }

    void solveMaster() {

        XPRBprob problem = bcl.newProb("MCF");      /* Create a new problem in BCL */
//        problem.reset();
//        XPRS.init();
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
                edgeConstraints.put(edge, problem.newCtr(expr.lEql((double) edge.getAttribute("capacity"))));
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
//            edge.setAttribute("originWeight", (double)edge.getAttribute("weight"));
            originWeightOfModifiedEdges.put(edge, edge.getAttribute("weight"));
            edge.setAttribute("weight", (double) edge.getAttribute("weight") + edgeDuals.get(edge));
//            graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) + edgeDuals.get(edge));
//            edge.setWeight(e.getWeight() + edgeDuals.get(e.getId()));
        }

        boolean noPathFound = true;

        for (Flow f : flows) {
            dijkstra.setSource(graph.getNode(f.from.getId()));
            dijkstra.compute();
//            pathFinder.compute();
            Path sp = dijkstra.getPath(graph.getNode(f.to.getId()));
            if (dijkstra.getPathLength(graph.getNode(f.to.getId())) < flowDuals.get(f)) {
                noPathFound = false;
                f.paths.add(sp);
            }
        }

        for (Edge edge : originWeightOfModifiedEdges.keySet()) {
            edge.setAttribute("weight", originWeightOfModifiedEdges.get(edge));
//            graph.setEdgeWeight(edge, edge.originWeight);
        }
        return noPathFound;
    }

}
