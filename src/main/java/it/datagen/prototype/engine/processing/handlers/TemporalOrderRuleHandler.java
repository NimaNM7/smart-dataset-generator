package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.rules.TemporalOrderRule;

import java.util.List;
import java.util.Map;

public class TemporalOrderRuleHandler implements RuleHandler<TemporalOrderRule> {

	@Override
	public Class<TemporalOrderRule> ruleType() {
		return TemporalOrderRule.class;
	}

	@Override
	public boolean validateValues(TemporalOrderRule rule, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context) {
		if (rule == null || rowValues == null) {
			return false;
		}

		Integer comparison = RuleValueUtils.compareValues(
				rowValues.get(rule.getEarlierDateColumn()),
				rowValues.get(rule.getLaterDateColumn())
		);
		if (comparison == null) {
			return false;
		}

		return rule.isAllowEqual() ? comparison <= 0 : comparison < 0;
	}

	@Override
	public boolean fixValues(TemporalOrderRule rule, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context) {
		if (validateValues(rule, rowValues, allRowValues, context)) {
			return true;
		}
		if (rule == null || rowValues == null) {
			return false;
		}

		Object earlierValue = rowValues.get(rule.getEarlierDateColumn());
		Object laterValue = rowValues.get(rule.getLaterDateColumn());
		Integer comparison = RuleValueUtils.compareValues(earlierValue, laterValue);
		if (comparison == null) {
			return false;
		}

		if (rule.isAllowEqual()) {
			rowValues.put(rule.getLaterDateColumn(), earlierValue);
			return true;
		}

		Object adjustedLater = RuleValueUtils.incrementStrictly(earlierValue);
		if (adjustedLater == null) {
			return false;
		}

		rowValues.put(rule.getLaterDateColumn(), adjustedLater);
		return true;
	}
}
