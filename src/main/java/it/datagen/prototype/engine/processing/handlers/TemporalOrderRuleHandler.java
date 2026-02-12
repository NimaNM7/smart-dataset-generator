package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.rules.TemporalOrderRule;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
			List<Map<String, Object>> allRowValues, RuleProcessingContext context, Set<String> fixedColumns) {
		if (rule == null || rowValues == null) {
			return false;
		}

		Object earlierValue = rowValues.get(rule.getEarlierDateColumn());
		Object laterValue = rowValues.get(rule.getLaterDateColumn());
		Integer comparison = RuleValueUtils.compareValues(earlierValue, laterValue);
		if (comparison == null) {
			return false;
		}

		boolean earlierFixed = fixedColumns.contains(rule.getEarlierDateColumn());
		boolean laterFixed = fixedColumns.contains(rule.getLaterDateColumn());

		if (!laterFixed) {
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

		if (earlierFixed) {
			return false;
		}

		if (rule.isAllowEqual()) {
			rowValues.put(rule.getEarlierDateColumn(), laterValue);
			return true;
		}

		Object adjustedEarlier = RuleValueUtils.decrementStrictly(laterValue);
		if (adjustedEarlier == null) {
			return false;
		}

		rowValues.put(rule.getEarlierDateColumn(), adjustedEarlier);
		return true;
	}
}
