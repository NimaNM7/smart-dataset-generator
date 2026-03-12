package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.models.RuleProcessingContext;
import it.datagen.prototype.model.rules.ColumnComparisonRule;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ColumnComparisonRuleHandler implements RuleHandler<ColumnComparisonRule> {

	@Override
	public Class<ColumnComparisonRule> ruleType() {
		return ColumnComparisonRule.class;
	}

	@Override
	public boolean validateValues(ColumnComparisonRule rule, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context) {
		if (rule == null || rowValues == null) {
			return false;
		}

		Integer comparison = compareValues(
				rowValues.get(rule.getSmallerColumn()),
				rowValues.get(rule.getLargerColumn())
		);
		if (comparison == null) {
			return false;
		}

		return rule.isAllowEqual() ? comparison <= 0 : comparison < 0;
	}

	@Override
	public boolean fixValues(ColumnComparisonRule rule, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context, Set<String> fixedColumns) {
		if (rule == null || rowValues == null) {
			return false;
		}

		Object smallerValue = rowValues.get(rule.getSmallerColumn());
		Object largerValue = rowValues.get(rule.getLargerColumn());
		Integer comparison = compareValues(smallerValue, largerValue);
		if (comparison == null) {
			return false;
		}

		boolean smallerFixed = fixedColumns.contains(rule.getSmallerColumn());
		boolean largerFixed = fixedColumns.contains(rule.getLargerColumn());

		if (!largerFixed) {
			if (rule.isAllowEqual()) {
				rowValues.put(rule.getLargerColumn(), smallerValue);
				return true;
			}
			Object nextValue = incrementStrictly(smallerValue);
			if (nextValue == null) {
				return false;
			}
			rowValues.put(rule.getLargerColumn(), nextValue);
			return true;
		}

		if (smallerFixed) {
			return false;
		}

		if (rule.isAllowEqual()) {
			rowValues.put(rule.getSmallerColumn(), largerValue);
			return true;
		}
		Object previousValue = decrementStrictly(largerValue);
		if (previousValue == null) {
			return false;
		}
		rowValues.put(rule.getSmallerColumn(), previousValue);
		return true;
	}

	private Integer compareValues(Object left, Object right) {
		return RuleValueUtils.compareValues(left, right);
	}

	private Object incrementStrictly(Object value) {
		return RuleValueUtils.incrementStrictly(value);
	}

	private Object decrementStrictly(Object value) {
		return RuleValueUtils.decrementStrictly(value);
	}
}
