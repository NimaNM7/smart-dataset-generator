package it.datagen.prototype.engine.processing;

import it.datagen.prototype.engine.processing.handlers.ColumnComparisonRuleHandler;
import it.datagen.prototype.engine.processing.handlers.ConditionalCompletenessRuleHandler;
import it.datagen.prototype.engine.processing.handlers.FunctionalDependencyRuleHandler;
import it.datagen.prototype.engine.processing.handlers.LengthComparisonRuleHandler;
import it.datagen.prototype.engine.processing.handlers.TemporalOrderRuleHandler;
import it.datagen.prototype.engine.processing.handlers.UniqueKeyRuleHandler;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.DataQualityRule;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dispatches rules to typed handlers.
 */
public class RuleProcessorRegistry {

	private final Map<Class<? extends DataQualityRule>, RuleHandler<? extends DataQualityRule>> handlersByRuleType;

	public RuleProcessorRegistry(List<RuleHandler<? extends DataQualityRule>> handlers) {
		Objects.requireNonNull(handlers, "Handlers cannot be null.");

		this.handlersByRuleType = new LinkedHashMap<>();
		for (RuleHandler<? extends DataQualityRule> handler : handlers) {
			Objects.requireNonNull(handler, "Handler cannot be null.");
			handlersByRuleType.put(handler.ruleType(), handler);
		}
	}

	public static RuleProcessorRegistry withDefaults() {
		return new RuleProcessorRegistry(Arrays.asList(
				new UniqueKeyRuleHandler(),
				new ColumnComparisonRuleHandler(),
				new TemporalOrderRuleHandler(),
				new LengthComparisonRuleHandler(),
				new ConditionalCompletenessRuleHandler(),
				new FunctionalDependencyRuleHandler()
		));
	}

	public boolean validate(DataQualityRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		RuleHandler<DataQualityRule> handler = resolveHandler(rule);
		if (handler == null) {
			return true;
		}
		return handler.validate(rule, row, allRows, context);
	}

	public boolean fix(DataQualityRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		RuleHandler<DataQualityRule> handler = resolveHandler(rule);
		if (handler == null) {
			return false;
		} 
		return handler.fix(rule, row, allRows, context);
	}

	@SuppressWarnings("unchecked")
	private RuleHandler<DataQualityRule> resolveHandler(DataQualityRule rule) {
		if (rule == null) {
			return null;
		}
		return (RuleHandler<DataQualityRule>) handlersByRuleType.get(rule.getClass());
	}
}
