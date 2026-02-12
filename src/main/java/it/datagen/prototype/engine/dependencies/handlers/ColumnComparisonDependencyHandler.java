package it.datagen.prototype.engine.dependencies.handlers;

import it.datagen.prototype.engine.dependencies.DependencyRuleHandler;
import it.datagen.prototype.model.rules.ColumnComparisonRule;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class ColumnComparisonDependencyHandler implements DependencyRuleHandler<ColumnComparisonRule> {

	@Override
	public Class<ColumnComparisonRule> ruleType() {
		return ColumnComparisonRule.class;
	}

	@Override
	public void addDependencies(ColumnComparisonRule rule, Graph<String, DefaultEdge> graph) {
		DependencyGraphUtils.addDependency(graph, rule.getSmallerColumn(), rule.getLargerColumn());
	}
}
