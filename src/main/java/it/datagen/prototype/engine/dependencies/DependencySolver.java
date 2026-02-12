package it.datagen.prototype.engine.dependencies;

import it.datagen.prototype.model.rules.DataQualityRule;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.List;
import java.util.Set;

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
        Graph<String, DefaultEdge> graph =
                new DefaultDirectedGraph<>(DefaultEdge.class);

        for (DataQualityRule rule : rules) {
            dependencyProcessors.addDependencies(rule, graph);
        }

        StrongConnectivityAlgorithm<String, DefaultEdge> sccAlg =
                new KosarajuStrongConnectivityInspector<>(graph);

        return sccAlg.stronglyConnectedSets();
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
