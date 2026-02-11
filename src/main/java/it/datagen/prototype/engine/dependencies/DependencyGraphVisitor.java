package it.datagen.prototype.engine.dependencies;

import it.datagen.prototype.model.rules.ColumnComparisonRule;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class DependencyGraphVisitor {

    private final Graph<String, DefaultEdge> graph;

    public DependencyGraphVisitor(Graph<String, DefaultEdge> graph) {
        this.graph = graph;
    }

    public void visit(ColumnComparisonRule rule) {
        graph.addVertex(rule.getSmallerColumn());
        graph.addVertex(rule.getLargerColumn());
        graph.addEdge(rule.getSmallerColumn(), rule.getLargerColumn());
    }

    // visit functions for other rules
}
