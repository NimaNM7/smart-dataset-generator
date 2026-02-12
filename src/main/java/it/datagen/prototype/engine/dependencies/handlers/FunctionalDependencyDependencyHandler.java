package it.datagen.prototype.engine.dependencies.handlers;

import it.datagen.prototype.engine.dependencies.DependencyRuleHandler;
import it.datagen.prototype.model.rules.FunctionalDependencyRule;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class FunctionalDependencyDependencyHandler implements DependencyRuleHandler<FunctionalDependencyRule> {

	@Override
	public Class<FunctionalDependencyRule> ruleType() {
		return FunctionalDependencyRule.class;
	}

	@Override
	public void addDependencies(FunctionalDependencyRule rule, Graph<String, DefaultEdge> graph) {
		for (String determinantColumn : rule.getDeterminantColumns()) {
			DependencyGraphUtils.addDependency(graph, determinantColumn, rule.getDependentColumn());
		}
	}
}
