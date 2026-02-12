package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.rules.LengthComparisonRule;

import java.util.List;
import java.util.Map;

public class LengthComparisonRuleHandler implements RuleHandler<LengthComparisonRule> {

	@Override
	public Class<LengthComparisonRule> ruleType() {
		return LengthComparisonRule.class;
	}

	@Override
	public boolean validateValues(LengthComparisonRule rule, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context) {
		if (rule == null || rowValues == null) {
			return false;
		}

		Object firstValue = rowValues.get(rule.getFirstColumn());
		Object secondValue = rowValues.get(rule.getSecondColumn());
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
	public boolean fixValues(LengthComparisonRule rule, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context) {
		if (rule == null || rowValues == null) {
			return false;
		}

		Object firstValue = rowValues.get(rule.getFirstColumn());
		if (!(firstValue instanceof CharSequence)) {
			return false;
		}

		String firstText = firstValue.toString();
		String secondText = toText(rowValues.get(rule.getSecondColumn()));

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

		rowValues.put(rule.getSecondColumn(), fixedSecond);
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
