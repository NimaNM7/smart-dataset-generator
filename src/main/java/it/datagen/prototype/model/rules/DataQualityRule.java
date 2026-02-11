package it.datagen.prototype.model.rules;

import it.datagen.prototype.engine.validation.RuleValidationVisitor;
import it.datagen.prototype.model.Row;

import java.util.List;

/**
 * Base abstraction for all data quality rules.
 */
public interface DataQualityRule {

	/**
	 * Accepts a validation visitor for this rule in the context of a row and dataset.
	 *
	 * @param row      the row to validate
	 * @param allRows  the dataset context (needed by some rules, e.g. functional dependencies)
	 * @param visitor  validation visitor containing the concrete validation logic
	 * @return true if the rule is satisfied for the given row, false otherwise
	 */
	boolean accept(RuleValidationVisitor visitor, Row row, List<Row> allRows);
}
