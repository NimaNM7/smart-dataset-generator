package it.datagen.prototype.engine.dependencies.handlers;

import it.datagen.prototype.engine.dependencies.DependencyRuleHandler;
import it.datagen.prototype.model.rules.LengthComparisonRule;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class LengthComparisonDependencyHandler implements DependencyRuleHandler<LengthComparisonRule> {

	@Override
	public Class<LengthComparisonRule> ruleType() {
		return LengthComparisonRule.class;
	}

	@Override
	public void addDependencies(LengthComparisonRule rule, Graph<String, DefaultEdge> graph) {
		DependencyGraphUtils.addDependency(graph, rule.getFirstColumn(), rule.getSecondColumn());
	}
}
