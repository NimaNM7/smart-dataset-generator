package it.datagen.prototype.engine.dependencies.handlers;

import it.datagen.prototype.engine.dependencies.DependencyRuleHandler;
import it.datagen.prototype.model.rules.ConditionalCompletenessRule;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class ConditionalCompletenessDependencyHandler implements DependencyRuleHandler<ConditionalCompletenessRule> {

	@Override
	public Class<ConditionalCompletenessRule> ruleType() {
		return ConditionalCompletenessRule.class;
	}

	@Override
	public void addDependencies(ConditionalCompletenessRule rule, Graph<String, DefaultEdge> graph) {
		DependencyGraphUtils.addDependency(graph, rule.getConditionColumn(), rule.getTargetColumn());
	}
}
