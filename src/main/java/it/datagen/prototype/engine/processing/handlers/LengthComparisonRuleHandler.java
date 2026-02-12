package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.LengthComparisonRule;

import java.util.List;

public class LengthComparisonRuleHandler implements RuleHandler<LengthComparisonRule> {

	@Override
	public Class<LengthComparisonRule> ruleType() {
		return LengthComparisonRule.class;
	}

	@Override
	public boolean validate(LengthComparisonRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (rule == null || row == null) {
			return false;
		}

		Object firstValue = row.getValue(rule.getFirstColumn());
		Object secondValue = row.getValue(rule.getSecondColumn());
		if (!(firstValue instanceof CharSequence) || !(secondValue instanceof CharSequence)) {
			return false;
		}

		int firstLength = ((CharSequence) firstValue).length();
		int secondLength = ((CharSequence) secondValue).length();
        return switch (rule.getComparison()) {
            case SHORTER_THAN_OR_EQUAL -> firstLength <= secondLength;
            case LONGER_THAN_OR_EQUAL -> firstLength >= secondLength;
            case STRICTLY_SHORTER_THAN -> firstLength < secondLength;
            case STRICTLY_LONGER_THAN -> firstLength > secondLength;
        };
	}

	@Override
	public boolean fix(LengthComparisonRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (validate(rule, row, allRows, context)) {
			return true;
		}
		if (rule == null || row == null) {
			return false;
		}

		Object firstValue = row.getValue(rule.getFirstColumn());
		if (!(firstValue instanceof CharSequence)) {
			return false;
		}

		String firstText = firstValue.toString();
		String secondText = toText(row.getValue(rule.getSecondColumn()));

		String fixedSecond;
		switch (rule.getComparison()) {
			case SHORTER_THAN_OR_EQUAL:
				fixedSecond = ensureMinLength(secondText, firstText.length(), firstText);
				break;
			case LONGER_THAN_OR_EQUAL:
				fixedSecond = truncateToMaxLength(secondText, firstText.length());
				break;
			case STRICTLY_SHORTER_THAN:
				fixedSecond = ensureMinLength(secondText, firstText.length() + 1, firstText + "_");
				break;
			case STRICTLY_LONGER_THAN:
				if (firstText.isEmpty()) {
					return false;
				}
				fixedSecond = truncateToMaxLength(secondText, firstText.length() - 1);
				break;
			default:
				return false;
		}

		row.setValue(rule.getSecondColumn(), fixedSecond);
		return true;
	}

	private String toText(Object value) {
		return value == null ? "" : value.toString();
	}

	private String truncateToMaxLength(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	private String ensureMinLength(String value, int minLength, String seed) {
		if (value.length() >= minLength) {
			return value;
		}

		StringBuilder builder = new StringBuilder(seed == null ? "" : seed);
		if (builder.isEmpty()) {
			builder.append('_');
		}
		while (builder.length() < minLength) {
			builder.append('_');
		}
		return builder.toString();
	}
}
