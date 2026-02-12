package it.datagen.prototype.engine.processing;

import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.DataQualityRule;

import java.util.List;

/**
 * A typed processor for one rule type.
 */
public interface RuleHandler<R extends DataQualityRule> {

	Class<R> ruleType();

	default boolean validate(R rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		return true;
	}

	default boolean fix(R rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		return false;
	}
}
