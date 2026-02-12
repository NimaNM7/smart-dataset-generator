package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.TemporalOrderRule;

import java.util.List;

public class TemporalOrderRuleHandler implements RuleHandler<TemporalOrderRule> {

	@Override
	public Class<TemporalOrderRule> ruleType() {
		return TemporalOrderRule.class;
	}

	@Override
	public boolean validate(TemporalOrderRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (rule == null || row == null) {
			return false;
		}

		Integer comparison = RuleValueUtils.compareValues(
				row.getValue(rule.getEarlierDateColumn()),
				row.getValue(rule.getLaterDateColumn())
		);
		if (comparison == null) {
			return false;
		}

		return rule.isAllowEqual() ? comparison <= 0 : comparison < 0;
	}

	@Override
	public boolean fix(TemporalOrderRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (validate(rule, row, allRows, context)) {
			return true;
		}
		if (rule == null || row == null) {
			return false;
		}

		Object earlierValue = row.getValue(rule.getEarlierDateColumn());
		Object laterValue = row.getValue(rule.getLaterDateColumn());
		Integer comparison = RuleValueUtils.compareValues(earlierValue, laterValue);
		if (comparison == null) {
			return false;
		}

		if (rule.isAllowEqual()) {
			row.setValue(rule.getLaterDateColumn(), earlierValue);
			return true;
		}

		Object adjustedLater = RuleValueUtils.incrementStrictly(earlierValue);
		if (adjustedLater == null) {
			return false;
		}

		row.setValue(rule.getLaterDateColumn(), adjustedLater);
		return true;
	}
}
