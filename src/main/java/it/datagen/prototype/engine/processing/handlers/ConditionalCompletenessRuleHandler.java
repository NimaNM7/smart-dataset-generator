package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.ConditionalCompletenessRule;

import java.util.List;
import java.util.Objects;

public class ConditionalCompletenessRuleHandler implements RuleHandler<ConditionalCompletenessRule> {

	private static final String DEFAULT_FILLED_TEXT = "AUTO_FILLED";

	@Override
	public Class<ConditionalCompletenessRule> ruleType() {
		return ConditionalCompletenessRule.class;
	}

	@Override
	public boolean validate(ConditionalCompletenessRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (rule == null || row == null) {
			return false;
		}

		Object conditionValue = row.getValue(rule.getConditionColumn());
		if (!Objects.equals(conditionValue, rule.getConditionValue())) {
			return true;
		}

		Object targetValue = row.getValue(rule.getTargetColumn());
		return rule.isTargetMustBeNull() == (targetValue == null);
	}

	@Override
	public boolean fix(ConditionalCompletenessRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (validate(rule, row, allRows, context)) {
			return true;
		}
		if (rule == null || row == null) {
			return false;
		}

		Object conditionValue = row.getValue(rule.getConditionColumn());
		if (!Objects.equals(conditionValue, rule.getConditionValue())) {
			return true;
		}

		if (rule.isTargetMustBeNull()) {
			row.setValue(rule.getTargetColumn(), null);
			return true;
		}

		Object targetValue = row.getValue(rule.getTargetColumn());
		if (targetValue != null) {
			return true;
		}

		Object replacement = rule.getConditionValue() != null ? rule.getConditionValue() : DEFAULT_FILLED_TEXT;
		row.setValue(rule.getTargetColumn(), replacement);
		return true;
	}
}
