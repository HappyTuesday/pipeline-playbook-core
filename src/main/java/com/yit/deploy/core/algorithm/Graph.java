package com.yit.deploy.core.algorithm;

import com.yit.deploy.core.function.Lambda;

import java.util.*;
import java.util.function.Consumer;

public class Graph<N, A> {
    /**
     * node name -> node
     */
    private final Map<String, Node<N>> nodes = new HashMap<>();
    /**
     * start node name -> end node name -> arc data
     */
    private final Map<String, Map<String, A>> arcs = new HashMap<>();

    public Graph<N, A> node(String name, N data) {
        if (nodes.containsKey(name)) {
            nodes.get(name).data = data;
        } else {
            nodes.put(name, new Node<>(name, data));
        }
        return this;
    }

    public boolean containsNode(String name) {
        return nodes.containsKey(name);
    }

    public N getAt(String name) {
        Node<N> node = nodes.get(name);
        return node == null ? null : node.data;
    }

    public N putAt(String name, N data) {
        if (nodes.containsKey(name)) {
            nodes.get(name).data = data;
            return data;
        } else {
            throw new IllegalArgumentException(name);
        }
    }

    public Graph<N, A> arc(String from, String to) {
        return arc(from, to, null);
    }

    public Graph<N, A> arc(String from, String to, A arcData) {
        Node<N> fromNode, toNode;
        if (nodes.containsKey(from)) {
            fromNode = nodes.get(from);
        } else {
            fromNode = new Node<>(from, null);
            nodes.put(from, fromNode);
        }

        if (nodes.containsKey(to)) {
            toNode = nodes.get(to);
        } else {
            toNode = new Node<>(to, null);
            nodes.put(to, toNode);
        }

        if (!fromNode.next.contains(toNode)) fromNode.next.add(toNode);
        if (!toNode.prev.contains(fromNode)) toNode.prev.add(fromNode);

        arcs.computeIfAbsent(from, n -> new HashMap<>()).put(to, arcData);

        return this;
    }

    public A getArc(String from, String to) {
        Map<String, A> m = arcs.get(from);
        return m != null ? m.get(to) : null;
    }

    public Graph<N, A> removeNode(String name) {
        Node<N> n = nodes.get(name);
        if (n != null) {
            for (Node m : n.prev) {
                m.next.remove(n);
            }
            for (Node m : n.next) {
                m.prev.remove(n);
            }
            nodes.remove(name);
        }
        return this;
    }

    public List<String> prev(String name) {
        if (!nodes.containsKey(name)) throw new IllegalArgumentException(name);
        return Lambda.map(nodes.get(name).prev, n -> n.name);
    }

    public List<String> next(String name) {
        if (!nodes.containsKey(name)) throw new IllegalArgumentException(name);
        return Lambda.map(nodes.get(name).next, n -> n.name);
    }

    public Graph<N, A> removeArc(String from, String to) {
        Node fromNode, toNode;
        if (nodes.containsKey(from) && nodes.containsKey(to)) {
            fromNode = nodes.get(from);
            toNode = nodes.get(to);
            fromNode.next.remove(toNode);
            toNode.prev.remove(fromNode);
        }
        return this;
    }

    private Graph<N, A> fork() {
        Graph<N, A> g = new Graph<>();
        for (Node<N> n : nodes.values()) {
            g.node(n.name, n.data);
        }
        for (Node<N> n : nodes.values()) {
            for (Node<N> m : n.next) {
                g.arc(n.name, m.name);
            }
        }
        return g;
    }

    public List<String> topology() {
        Graph<N, A> g = fork();
        List<Node<N>> open = new ArrayList<>(g.nodes.values());
        List<String> result = new ArrayList<>();
        while (!open.isEmpty()) {
            Node<N> node = null;
            for (Node<N> n : open) {
                if (n.prev.isEmpty()) {
                    node = n;
                    break;
                }
            }
            if (node == null) {
                throw new IllegalArgumentException("circle find in graph " + Lambda.toString(Lambda.map(open, n -> n.name)));
            }
            for (Node<N> n : node.next) {
                n.prev.remove(node);
            }
            open.remove(node);
            result.add(node.name);
        }
        return result;
    }

    public Graph reverse() {
        Graph<N, A> g = new Graph<>();
        for (Node<N> n : nodes.values()) {
            g.node(n.name, n.data);
            for (Node m : n.next) {
                g.arc(m.name, n.name);
            }
        }

        return g;
    }

    /**
     * travel to all directly / indirectly previous nodes of those nodes which are the directly or indirectly next nodes of the starter node (includes the starter node).
     * @param starter the starter node
     * @param visitor apply this lambda to all the visited nodes
     */
    public void travelForest(String starter, Visitor<N, A> visitor) {
        Set<String> close = new HashSet<>();
        travel(nodes.get(starter), Direction.forward, new HashSet<>(), s1 -> {
            if (visitor.visit(TravelingStep.unwrap(s1))) {
                travel(s1.node, Direction.backward, close, s2 -> {
                    if (s1.node == s2.node) {
                        return true;
                    }
                    return visitor.visit(TravelingStep.unwrap(s2));
                });
                return true;
            }
            return false;
        });
    }

    public void travelForward(String starter, Visitor<N, A> visitor) {
        travel(nodes.get(starter), Direction.forward, new HashSet<>(), s -> visitor.visit(TravelingStep.unwrap(s)));
    }

    public void travelBackward(String starter, Visitor<N, A> visitor) {
        travel(nodes.get(starter), Direction.backward, new HashSet<>(), s -> visitor.visit(TravelingStep.unwrap(s)));
    }

    private void travel(Node<N> starter, Direction direction, Set<String> close, Visitor<Node<N>, A> visitor) {
        Stack<TravelingStep<Node<N>, A>> open = new Stack<>();
        open.push(new TravelingStep<>(starter, null, null, null));
        while (!open.isEmpty()) {
            TravelingStep<Node<N>, A> step = open.pop();

            if (!close.add(step.node.name)) {
                continue;
            }
            if (!visitor.visit(step)) {
                continue;
            }
            List<Node<N>> list = direction == Direction.forward ? step.node.next : step.node.prev;
            for (int i = list.size() - 1; i >= 0; i--) {
                Node<N> n = list.get(i);
                A arc = direction == Direction.forward ? getArc(step.node.name, n.name) : getArc(n.name, step.node.name);
                open.add(new TravelingStep<>(n, step.node, direction, arc));
            }
        }
    }

    @Override
    public String toString() {
        return Lambda.join("\n", nodes.values());
    }

    public enum Direction {
        forward,
        backward
    }

    private static class Node<N> {
        final String name;
        N data;
        final List<Node<N>> prev = new ArrayList<>();
        final List<Node<N>> next = new ArrayList<>();

        public Node(String name, N data) {
            this.name = name;
            this.data = data;
        }

        @Override
        public String toString() {
            return Lambda.toString(Lambda.map(prev, n -> n.name)) + " -> (" + name + ") -> " + Lambda.toString(Lambda.map(next, n->n.name));
        }
    }

    public static class TravelingStep<N, A> {
        public final N node;
        public final N from;
        public final Direction direction;
        public final A arc;

        private TravelingStep(N node, N from, Direction direction, A arc) {
            this.node = node;
            this.from = from;
            this.direction = direction;
            this.arc = arc;
        }

        public static <N, A> TravelingStep<N, A> unwrap(TravelingStep<Node<N>, A> step) {
            return new TravelingStep<>(step.node.data, step.from != null ? step.from.data : null, step.direction, step.arc);
        }
    }

    @FunctionalInterface
    public interface Visitor<N, A> {
        /**
         * the visit used while traveling
         * @param step current step we are traveling now
         * @return return true means also visiting all the succession nodes
         */
        boolean visit(TravelingStep<N, A> step);
    }
}