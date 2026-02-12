package it.datagen.prototype.engine.dependencies;

import it.datagen.prototype.model.rules.DataQualityRule;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencySolver {

    private final DependencyProcessorRegistry dependencyProcessors;

    public DependencySolver() {
        this(DependencyProcessorRegistry.withDefaults());
    }

    public DependencySolver(DependencyProcessorRegistry dependencyProcessors) {
        this.dependencyProcessors = dependencyProcessors;
    }

    public List<Set<String>> solve(
            // TableSchema schema, (it may be needed later, but for now we delete it)
            List<DataQualityRule> rules
    ) {
        Graph<String, DefaultEdge> graph = buildDependencyGraph(rules);
        StrongConnectivityAlgorithm<String, DefaultEdge> sccAlg = new KosarajuStrongConnectivityInspector<>(graph);
        return sccAlg.stronglyConnectedSets();
    }

    /**
     * Computes SCCs and returns them in a deterministic topological order of the SCC DAG.
     *
     * <p>Note: {@code stronglyConnectedSets()} from JGraphT's SCC algorithm may appear ordered
     * for a specific implementation, but ordering is not part of the interface contract.
     * This method explicitly builds and topologically sorts the condensation graph.
     */
    public List<Set<String>> solveTopologically(List<DataQualityRule> rules) {
        Graph<String, DefaultEdge> graph = buildDependencyGraph(rules);
        StrongConnectivityAlgorithm<String, DefaultEdge> sccAlg = new KosarajuStrongConnectivityInspector<>(graph);
        List<Set<String>> sccs = sccAlg.stronglyConnectedSets();
        if (sccs.isEmpty()) {
            return sccs;
        }

        Map<String, Integer> componentByColumn = new HashMap<>();
        for (int componentIndex = 0; componentIndex < sccs.size(); componentIndex++) {
            for (String columnName : sccs.get(componentIndex)) {
                componentByColumn.put(columnName, componentIndex);
            }
        }

        Map<Integer, Set<Integer>> outgoing = new HashMap<>();
        Map<Integer, Integer> indegree = new HashMap<>();
        for (int componentIndex = 0; componentIndex < sccs.size(); componentIndex++) {
            outgoing.put(componentIndex, new HashSet<>());
            indegree.put(componentIndex, 0);
        }

        for (DefaultEdge edge : graph.edgeSet()) {
            String sourceColumn = graph.getEdgeSource(edge);
            String targetColumn = graph.getEdgeTarget(edge);
            Integer sourceComponent = componentByColumn.get(sourceColumn);
            Integer targetComponent = componentByColumn.get(targetColumn);
            if (sourceComponent == null || targetComponent == null || sourceComponent.equals(targetComponent)) {
                continue;
            }

            if (outgoing.get(sourceComponent).add(targetComponent)) {
                indegree.put(targetComponent, indegree.get(targetComponent) + 1);
            }
        }

        Comparator<Integer> byLabel = Comparator.comparing(componentIndex -> componentLabel(sccs.get(componentIndex)));
        PriorityQueue<Integer> ready = new PriorityQueue<>(byLabel);
        for (int componentIndex = 0; componentIndex < sccs.size(); componentIndex++) {
            if (indegree.get(componentIndex) == 0) {
                ready.add(componentIndex);
            }
        }

        List<Set<String>> ordered = new ArrayList<>(sccs.size());
        while (!ready.isEmpty()) {
            Integer current = ready.poll();
            ordered.add(sccs.get(current));
            for (Integer next : outgoing.get(current)) {
                int newIndegree = indegree.get(next) - 1;
                indegree.put(next, newIndegree);
                if (newIndegree == 0) {
                    ready.add(next);
                }
            }
        }

        if (ordered.size() == sccs.size()) {
            return ordered;
        }

        return sccs;
    }

    private Graph<String, DefaultEdge> buildDependencyGraph(List<DataQualityRule> rules) {
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (DataQualityRule rule : rules) {
            dependencyProcessors.addDependencies(rule, graph);
        }
        return graph;
    }

    private String componentLabel(Set<String> componentColumns) {
        return componentColumns.stream().sorted().collect(Collectors.joining("|"));
    }

//    // for testing uncomment this
//    public static void main(String[] args) {
//        DependencySolver solver = new DependencySolver();
//        ColumnComparisonRule rule1 = new ColumnComparisonRule("A", "B", false);
//        ColumnComparisonRule rule2 = new ColumnComparisonRule("B", "C", false);
//        ColumnComparisonRule rule3 = new ColumnComparisonRule("C", "A", false);
//        List<DataQualityRule> rules = new ArrayList<>();
//        rules.add(rule1);
//        rules.add(rule2);
//        rules.add(rule3);
//        List<Set<String>> solved = solver.solve(rules);
//        System.out.println(solved);
//    }
}
