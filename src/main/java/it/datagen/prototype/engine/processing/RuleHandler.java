package it.datagen.prototype.engine.processing;

import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.DataQualityRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A typed processor for one rule type.
 *
 * <p>The canonical tool methods are map-based ({@code validateValues}/{@code fixValues})
 * so different execution strategies can work on full rows or partial row state.
 * Row-based methods are compatibility wrappers.
 */
public interface RuleHandler<R extends DataQualityRule> {

	Class<R> ruleType();

	default boolean validate(R rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		return validateValues(rule, row == null ? null : row.getValues(), toValueViews(allRows), context);
	}

	default boolean fix(R rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		return fixValues(rule, row == null ? null : row.getValues(), toValueViews(allRows), context);
	}

	default boolean validateValues(R rule, Map<String, Object> rowValues, List<Map<String, Object>> allRowValues,
			RuleProcessingContext context) {
		return true;
	}

	default boolean fixValues(R rule, Map<String, Object> rowValues, List<Map<String, Object>> allRowValues,
			RuleProcessingContext context) {
		return false;
	}

	private static List<Map<String, Object>> toValueViews(List<Row> rows) {
		if (rows == null) {
			return null;
		}
		List<Map<String, Object>> valueViews = new ArrayList<>(rows.size());
		for (Row row : rows) {
			valueViews.add(row == null ? null : row.getValues());
		}
		return valueViews;
	}
}
