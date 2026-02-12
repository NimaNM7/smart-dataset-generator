package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.ColumnComparisonRule;

import java.util.List;

public class ColumnComparisonRuleHandler implements RuleHandler<ColumnComparisonRule> {

	@Override
	public Class<ColumnComparisonRule> ruleType() {
		return ColumnComparisonRule.class;
	}

	@Override
	public boolean validate(ColumnComparisonRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (rule == null || row == null) {
			return false;
		}

		Integer comparison = compareValues(
				row.getValue(rule.getSmallerColumn()),
				row.getValue(rule.getLargerColumn())
		);
		if (comparison == null) {
			return false;
		}

		return rule.isAllowEqual() ? comparison <= 0 : comparison < 0;
	}

	@Override
	public boolean fix(ColumnComparisonRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (validate(rule, row, allRows, context)) {
			return true;
		}

		Object smallerValue = row.getValue(rule.getSmallerColumn());
		Object largerValue = row.getValue(rule.getLargerColumn());
		Integer comparison = compareValues(smallerValue, largerValue);
		if (comparison == null) {
			return false;
		}

		if (rule.isAllowEqual()) {
			row.setValue(rule.getLargerColumn(), smallerValue);
			return true;
		}

		Object nextValue = incrementStrictly(smallerValue);
		if (nextValue == null) {
			return false;
		}

		row.setValue(rule.getLargerColumn(), nextValue);
		return true;
	}

	private Integer compareValues(Object left, Object right) {
		return RuleValueUtils.compareValues(left, right);
	}

	private Object incrementStrictly(Object value) {
		return RuleValueUtils.incrementStrictly(value);
	}
}
