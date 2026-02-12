package it.datagen.prototype.engine.dependencies;

import it.datagen.prototype.model.rules.DataQualityRule;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public interface DependencyRuleHandler<R extends DataQualityRule> {

	Class<R> ruleType();

	void addDependencies(R rule, Graph<String, DefaultEdge> graph);
}
