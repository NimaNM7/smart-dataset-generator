package it.datagen.prototype.engine.dependencies;

import it.datagen.prototype.model.rules.ColumnComparisonRule;
import it.datagen.prototype.model.rules.ConditionalCompletenessRule;
import it.datagen.prototype.model.rules.FunctionalDependencyRule;
import it.datagen.prototype.model.rules.LengthComparisonRule;
import it.datagen.prototype.model.rules.TemporalOrderRule;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class DependencyGraphVisitor {

    private final Graph<String, DefaultEdge> graph;

    public DependencyGraphVisitor(Graph<String, DefaultEdge> graph) {
        this.graph = graph;
    }

    public void visit(ColumnComparisonRule rule) {
        addDependency(rule.getSmallerColumn(), rule.getLargerColumn());
    }

    public void visit(TemporalOrderRule rule) {
        addDependency(rule.getEarlierDateColumn(), rule.getLaterDateColumn());
    }

    public void visit(LengthComparisonRule rule) {
        addDependency(rule.getFirstColumn(), rule.getSecondColumn());
    }

    public void visit(ConditionalCompletenessRule rule) {
        addDependency(rule.getConditionColumn(), rule.getTargetColumn());
    }

    public void visit(FunctionalDependencyRule rule) {
        for (String determinant : rule.getDeterminantColumns()) {
            addDependency(determinant, rule.getDependentColumn());
        }
    }

    private void addDependency(String sourceColumn, String targetColumn) {
        graph.addVertex(sourceColumn);
        graph.addVertex(targetColumn);
        graph.addEdge(sourceColumn, targetColumn);
    }
}
