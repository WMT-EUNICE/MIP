package org.or.mip.vrp;

import java.util.*;

public class ElementaryShortestPath {
    int capacity = 10;

    class Node {
        int id;
        int earliestTime;
        int latestTime;
        int demand;

        public Node(int id, int earliestTime, int latestTime, int demand) {
            this.id = id;
            this.earliestTime = earliestTime;
            this.latestTime = latestTime;
            this.demand = demand;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "id=" + id +
                    ", earliestTime=" + earliestTime +
                    ", latestTime=" + latestTime +
                    ", demand=" + demand +
                    '}';
        }
    }

    class Arc {
        Node source;
        Node target;
        double cost;
        int time;

        public Arc(Node source, Node target, double cost, int time) {
            this.source = source;
            this.target = target;
            this.cost = cost;
            this.time = time;
        }

        @Override
        public String toString() {
            return "Arc{" +
                    "source=" + source.id +
                    ", target=" + target.id +
                    ", cost=" + cost +
                    ", time=" + time +
                    '}';
        }
    }

    class Label implements Comparable<Label> {
        Node node;
        Node pre;
        double cost;
        int arrivalTime;
        int curDemand;

        public Label(Node node, Node pre, double cost, int arrivalTime, int curDemand) {
            this.node = node;
            this.pre = pre;
            this.cost = cost;
            this.arrivalTime = arrivalTime;
            this.curDemand = curDemand;
        }

        @Override
        public int compareTo(Label o) {
            if (this.cost < o.cost) {
                return -1;
            } else if (this.cost > o.cost) {
                return 1;
            } else {
                return 0;
            }

        }

        @Override
        public String toString() {
            return "Label{" +
                    "node=" + node +
                    '}';
        }
    }

    List<Label> NPS = new LinkedList<>();

    Map<Node, List<Label>> nodeLabels = new LinkedHashMap<>();

    Map<Node, List<Arc>> outArcs = new LinkedHashMap<>();

    Map<Integer, Node> nodes = new LinkedHashMap<>();

    public void init() {
        Node n0 = new Node(0, 0, 0, 0);
        Node n1 = new Node(1, 6, 14, 0);
        Node n2 = new Node(2, 9, 12, 0);
        Node n3 = new Node(3, 8, 12, 0);
        Node n4 = new Node(4, 9, 35, 0);
        Node n5 = new Node(5, 15, 25, 0);
        nodes.put(0, n0);
        nodes.put(1, n1);
        nodes.put(2, n2);
        nodes.put(3, n3);
        nodes.put(4, n4);
        nodes.put(5, n5);

        outArcs.put(n0, new LinkedList<>());
        outArcs.get(n0).add(new Arc(n0, n1, 3, 8));
        outArcs.get(n0).add(new Arc(n0, n2, 5, 5));
        outArcs.get(n0).add(new Arc(n0, n3, 2, 12));

        outArcs.put(n1, new LinkedList<>());
        outArcs.get(n1).add(new Arc(n1, n4, 13, 4));
        outArcs.get(n1).add(new Arc(n1, n5, 6, 6));

        outArcs.put(n2, new LinkedList<>());
        outArcs.get(n2).add(new Arc(n2, n4, 8, 2));

        outArcs.put(n3, new LinkedList<>());
        outArcs.get(n3).add(new Arc(n3, n4, 16, 4));

        outArcs.put(n4, new LinkedList<>());

        outArcs.put(n5, new LinkedList<>());
        outArcs.get(n5).add(new Arc(n5, n4, 3, 7));

    }

    public void shortestPath(Node source, Node target) {
        Label sourceLabel = new Label(source, null, 0, 0, 0);
        nodeLabels.put(source, new LinkedList<>());
        nodeLabels.get(source).add(sourceLabel);
        NPS.add(sourceLabel);

        Node curNode = source;
        Label targetLabel;
//        NPS.remove(sourcelabel);
        while (curNode.id != target.id) {
            Collections.sort(NPS);
            targetLabel = NPS.get(0);
            NPS.remove(0);
            curNode = targetLabel.node;
            for (Arc arc : outArcs.get(targetLabel.node)) {
                if (arc.target.id != targetLabel.node.id &&
                        targetLabel.arrivalTime + arc.time <= arc.target.latestTime &&
                        targetLabel.curDemand + arc.target.demand <= capacity) {
                    if (!nodeLabels.containsKey(arc.target)) {
                        nodeLabels.put(arc.target, new LinkedList<>());
                        nodeLabels.get(arc.target).add(new Label(arc.target, curNode, targetLabel.cost + arc.cost, targetLabel.arrivalTime + arc.time,
                                targetLabel.curDemand + arc.target.demand));

                        NPS.add(nodeLabels.get(arc.target).get(0));
                    } else {
                        for (Label arcTargetLabel : nodeLabels.get(arc.target)) {
                            if (arcTargetLabel.cost > targetLabel.cost + arc.cost) {
                                arcTargetLabel.cost = targetLabel.cost + arc.cost;
                                arcTargetLabel.arrivalTime = Math.max(arc.target.earliestTime, targetLabel.arrivalTime + arc.time);
                                arcTargetLabel.curDemand = targetLabel.curDemand + arc.target.demand;
                                arcTargetLabel.pre = curNode;
                                NPS.add(arcTargetLabel);
                                break;
                            }
                        }
                    }
                }
            }
        }
        System.out.println();
    }

    public static void main(String[] args) {
        ElementaryShortestPath sp = new ElementaryShortestPath();
        sp.init();
        sp.shortestPath(sp.nodes.get(0), sp.nodes.get(4));
    }
}
