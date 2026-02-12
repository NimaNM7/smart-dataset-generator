package it.datagen.prototype.engine.dependencies.handlers;

import it.datagen.prototype.engine.dependencies.DependencyRuleHandler;
import it.datagen.prototype.model.rules.TemporalOrderRule;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class TemporalOrderDependencyHandler implements DependencyRuleHandler<TemporalOrderRule> {

	@Override
	public Class<TemporalOrderRule> ruleType() {
		return TemporalOrderRule.class;
	}

	@Override
	public void addDependencies(TemporalOrderRule rule, Graph<String, DefaultEdge> graph) {
		DependencyGraphUtils.addDependency(graph, rule.getEarlierDateColumn(), rule.getLaterDateColumn());
	}
}
