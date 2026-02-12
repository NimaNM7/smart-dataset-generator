package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.rules.LengthComparisonRule;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
			List<Map<String, Object>> allRowValues, RuleProcessingContext context, Set<String> fixedColumns) {
		if (rule == null || rowValues == null) {
			return false;
		}

		boolean firstFixed = fixedColumns.contains(rule.getFirstColumn());
		boolean secondFixed = fixedColumns.contains(rule.getSecondColumn());
		if (firstFixed && secondFixed) {
			return false;
		}

		String firstText = toText(rowValues.get(rule.getFirstColumn()));
		String secondText = toText(rowValues.get(rule.getSecondColumn()));

		if (!secondFixed) {
			String fixedSecond = fixSecondValue(rule, firstText, secondText);
			if (fixedSecond == null) {
				return false;
			}
			rowValues.put(rule.getSecondColumn(), fixedSecond);
			return true;
		}

		String fixedFirst = fixFirstValue(rule, firstText, secondText);
		if (fixedFirst == null) {
			return false;
		}
		rowValues.put(rule.getFirstColumn(), fixedFirst);
		return true;
	}

	private String fixSecondValue(LengthComparisonRule rule, String firstText, String secondText) {
		return switch (rule.getComparison()) {
			case SHORTER_THAN_OR_EQUAL -> ensureMinLength(secondText, firstText.length(), firstText);
			case LONGER_THAN_OR_EQUAL -> truncateToMaxLength(secondText, firstText.length());
			case STRICTLY_SHORTER_THAN -> ensureMinLength(secondText, firstText.length() + 1, firstText + "_");
			case STRICTLY_LONGER_THAN -> firstText.isEmpty()
					? null
					: truncateToMaxLength(secondText, firstText.length() - 1);
		};
	}

	private String fixFirstValue(LengthComparisonRule rule, String firstText, String secondText) {
		return switch (rule.getComparison()) {
			case SHORTER_THAN_OR_EQUAL -> truncateToMaxLength(firstText, secondText.length());
			case LONGER_THAN_OR_EQUAL -> ensureMinLength(firstText, secondText.length(), firstText);
			case STRICTLY_SHORTER_THAN -> secondText.isEmpty()
					? null
					: truncateToMaxLength(firstText, secondText.length() - 1);
			case STRICTLY_LONGER_THAN -> ensureMinLength(firstText, secondText.length() + 1, firstText + "_");
		};
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
