package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.models.RuleProcessingContext;
import it.datagen.prototype.model.rules.ConditionalCompletenessRule;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ConditionalCompletenessRuleHandler implements RuleHandler<ConditionalCompletenessRule> {

	private static final String DEFAULT_FILLED_TEXT = "AUTO_FILLED";

	@Override
	public Class<ConditionalCompletenessRule> ruleType() {
		return ConditionalCompletenessRule.class;
	}

	@Override
	public boolean validateValues(ConditionalCompletenessRule rule, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context) {
		if (rule == null || rowValues == null) {
			return false;
		}

		Object conditionValue = rowValues.get(rule.getConditionColumn());
		if (!Objects.equals(conditionValue, rule.getConditionValue())) {
			return true;
		}

		Object targetValue = rowValues.get(rule.getTargetColumn());
		return rule.isTargetMustBeNull() == (targetValue == null);
	}

	@Override
	public boolean fixValues(ConditionalCompletenessRule rule, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context, Set<String> fixedColumns) {
		if (rule == null || rowValues == null) {
			return false;
		}

		Object conditionValue = rowValues.get(rule.getConditionColumn());
		if (!Objects.equals(conditionValue, rule.getConditionValue())) {
			return true;
		}

		if (fixedColumns.contains(rule.getTargetColumn())) {
			return false;
		}

		if (rule.isTargetMustBeNull()) {
			rowValues.put(rule.getTargetColumn(), null);
			return true;
		}

		Object targetValue = rowValues.get(rule.getTargetColumn());
		if (targetValue != null) {
			return true;
		}

		Object replacement = rule.getConditionValue() != null ? rule.getConditionValue() : DEFAULT_FILLED_TEXT;
		rowValues.put(rule.getTargetColumn(), replacement);
		return true;
	}
}
