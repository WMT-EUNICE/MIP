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
public class MultiCommodityFlow2 {
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

    XPRBprob problem = bcl.newProb("MCF");      /* Create a new problem in BCL */

    XPRBbasis basis;

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

    Map<Flow, Map<Path, XPRBvar>> x_f_p = new HashMap<>();
    XPRBctr obj;

    public MultiCommodityFlow2() {
        buildGraph("src/org/or/mip/cg/tiny_network_01");
        dijkstra.init(graph);
//        pathFinder = new DijkstraShortestPath(graph);
        initFlows();
    }

    public static void main(String[] args) {
        MultiCommodityFlow2 mcf = new MultiCommodityFlow2();
        mcf.columnGeneration();
    }

    void columnGeneration() {
//        boolean genNewCol = true;
        initMaster();
        solveMaster();
        while (!pricing()) {
            solveMaster();
//            if (pricing())
//                genNewCol = false;
        }
        problem.lpOptimise();
//        System.out.println("Obj " + problem.getObjVal());
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

    void initMaster() {
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
        XPRBexpr objExpr = new XPRBexpr();
        for (Flow f : flows) {
            for (Path p : f.paths) {
                objExpr.add(x_f_p.get(f).get(p).mul(p.getPathWeight("weight")));
            }
        }
        obj = problem.newCtr(objExpr);
        problem.setObj(obj);

        //flow conservation
        for (Flow f : flows) {
            XPRBexpr expr = new XPRBexpr();
            for (Path p : f.paths) {
                expr.add(x_f_p.get(f).get(p));
            }
            flowConstraints.put(f, problem.newCtr(expr.eql(f.volume)));
        }

        //arc capacity
        for (Edge edge : graph.getEdgeSet()) {
            XPRBexpr expr = new XPRBexpr();
            for (Flow f : flows) {
                for (Path p : f.paths) {
                    if (p.getEdgePath().contains(edge)) {
                        expr.add(x_f_p.get(f).get(p));
                    }
                }
            }
            edgeConstraints.put(edge, problem.newCtr(expr.lEql((double) edge.getAttribute("capacity"))));
        }

        problem.setSense(XPRB.MINIM);
    }

    void solveMaster() {
        problem.lpOptimize("");             /* Solve the LP-problem */
        System.out.println("Objective: " + problem.getObjVal());  /* Get objective value */

        basis = problem.saveBasis();

        double totalCost = 0;
        for (Flow f : flows) {
            for (Path p : f.paths) {
                System.out.println(f.from + "-" + f.to + ", " + p.toString() + ", " + x_f_p.get(f).get(p).getSol());
                totalCost += x_f_p.get(f).get(p).getSol() * p.getPathWeight("weight");
            }
        }

        System.out.println("Total Cost: " + totalCost);

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
//            if (!edgeDuals.containsKey(edge))
//                continue;
            originWeightOfModifiedEdges.put(edge, edge.getAttribute("weight"));
            edge.setAttribute("weight", (double) edge.getAttribute("weight") + edgeDuals.get(edge));
        }

        boolean noPathFound = true;

        for (Flow f : flows) {

//
            dijkstra.setSource(graph.getNode(f.from.getId()));
            dijkstra.compute();
            Path sp = dijkstra.getPath(graph.getNode(f.to.getId()));
            if (dijkstra.getPathLength(graph.getNode(f.to.getId())) < flowDuals.get(f)) {

                XPRBvar var = problem.newVar(XPRB.PL);
                var.setLB(0);
                x_f_p.get(f).put(sp, var);

                obj.add(var.mul(dijkstra.getPathLength(graph.getNode(f.to.getId()))));

                //update flow conservation constraint;
                flowConstraints.get(f).add(var);

                //update arc capacity constraint
                for (Edge edge : graph.getEdgeSet()) {
                    if (sp.getEdgePath().contains(edge)) {
                        edgeConstraints.get(edge).add(var);
                    }
                }
                noPathFound = false;
                f.paths.add(sp);
            }
        }

        for (Edge edge : originWeightOfModifiedEdges.keySet()) {
            edge.setAttribute("weight", originWeightOfModifiedEdges.get(edge));
        }

        if (!noPathFound) {
            problem.loadMat();                    /* Reload the problem */
            problem.loadBasis(basis);             /* Load the saved basis */
            basis = null;                     /* No need to keep the basis any longer */
        }

        return noPathFound;
    }

}
