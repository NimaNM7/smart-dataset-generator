package it.datagen.prototype.engine.dependencies;

import it.datagen.prototype.engine.dependencies.handlers.ColumnComparisonDependencyHandler;
import it.datagen.prototype.engine.dependencies.handlers.ConditionalCompletenessDependencyHandler;
import it.datagen.prototype.engine.dependencies.handlers.FunctionalDependencyDependencyHandler;
import it.datagen.prototype.engine.dependencies.handlers.LengthComparisonDependencyHandler;
import it.datagen.prototype.engine.dependencies.handlers.TemporalOrderDependencyHandler;
import it.datagen.prototype.model.rules.DataQualityRule;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DependencyProcessorRegistry {

	private final Map<Class<? extends DataQualityRule>, DependencyRuleHandler<? extends DataQualityRule>> handlersByRuleType;

	public DependencyProcessorRegistry(List<DependencyRuleHandler<? extends DataQualityRule>> handlers) {
		Objects.requireNonNull(handlers, "Handlers cannot be null.");

		this.handlersByRuleType = new LinkedHashMap<>();
		for (DependencyRuleHandler<? extends DataQualityRule> handler : handlers) {
			Objects.requireNonNull(handler, "Handler cannot be null.");
			handlersByRuleType.put(handler.ruleType(), handler);
		}
	}

	public static DependencyProcessorRegistry withDefaults() {
		return new DependencyProcessorRegistry(Arrays.asList(
				new ColumnComparisonDependencyHandler(),
				new TemporalOrderDependencyHandler(),
				new LengthComparisonDependencyHandler(),
				new ConditionalCompletenessDependencyHandler(),
				new FunctionalDependencyDependencyHandler()
		));
	}

	public void addDependencies(DataQualityRule rule, Graph<String, DefaultEdge> graph) {
		DependencyRuleHandler<DataQualityRule> handler = resolveHandler(rule);
		if (handler == null) {
			return;
		}
		handler.addDependencies(rule, graph);
	}

	@SuppressWarnings("unchecked")
	private DependencyRuleHandler<DataQualityRule> resolveHandler(DataQualityRule rule) {
		if (rule == null) {
			return null;
		}
		return (DependencyRuleHandler<DataQualityRule>) handlersByRuleType.get(rule.getClass());
	}
}
