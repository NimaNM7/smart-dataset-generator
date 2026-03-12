package it.datagen.prototype.engine.processing;

import it.datagen.prototype.engine.models.RuleProcessingContext;
import it.datagen.prototype.engine.processing.handlers.ColumnComparisonRuleHandler;
import it.datagen.prototype.engine.processing.handlers.ConditionalCompletenessRuleHandler;
import it.datagen.prototype.engine.processing.handlers.FunctionalDependencyRuleHandler;
import it.datagen.prototype.engine.processing.handlers.LengthComparisonRuleHandler;
import it.datagen.prototype.engine.processing.handlers.TemporalOrderRuleHandler;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.DataQualityRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
				new ColumnComparisonRuleHandler(),
				new TemporalOrderRuleHandler(),
				new LengthComparisonRuleHandler(),
				new ConditionalCompletenessRuleHandler(),
				new FunctionalDependencyRuleHandler()
		));
	}

	public boolean validate(DataQualityRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		return validate(rule, row == null ? null : row.getValues(), toValueViews(allRows), context);
	}

	public boolean validate(DataQualityRule rule, Map<String, Object> rowValues, List<Map<String, Object>> allRowValues,
			RuleProcessingContext context) {
		RuleHandler<DataQualityRule> handler = resolveHandler(rule);
		if (handler == null) {
			return true;
		}
		return handler.validateValues(rule, rowValues, allRowValues, context);
	}

	public boolean fix(DataQualityRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		return fix(rule, row, allRows, context, Collections.emptySet());
	}

	public boolean fix(DataQualityRule rule, Row row, List<Row> allRows, RuleProcessingContext context,
			Set<String> fixedColumns) {
		return fix(rule, row == null ? null : row.getValues(), toValueViews(allRows), context, fixedColumns);
	}

	public boolean fix(DataQualityRule rule, Map<String, Object> rowValues, List<Map<String, Object>> allRowValues,
			RuleProcessingContext context) {
		return fix(rule, rowValues, allRowValues, context, Collections.emptySet());
	}

	public boolean fix(DataQualityRule rule, Map<String, Object> rowValues, List<Map<String, Object>> allRowValues,
			RuleProcessingContext context, Set<String> fixedColumns) {
		RuleHandler<DataQualityRule> handler = resolveHandler(rule);
		if (handler == null) {
			return false;
		}
		return handler.fixValues(rule, rowValues, allRowValues, context, fixedColumns);
	}

	@SuppressWarnings("unchecked")
	private RuleHandler<DataQualityRule> resolveHandler(DataQualityRule rule) {
		if (rule == null) {
			return null;
		}
		return (RuleHandler<DataQualityRule>) handlersByRuleType.get(rule.getClass());
	}

	private List<Map<String, Object>> toValueViews(List<Row> rows) {
		if (rows == null) {
			return null;
		}
		List<Map<String, Object>> valueViews = new ArrayList<>(rows.size());
		for (Row row : rows) {
			valueViews.add(row == null ? null : row.getValues());
		}
		return valueViews;
	}
}
