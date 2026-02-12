package it.datagen.prototype.engine;

import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.engine.processing.RuleProcessorRegistry;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.TableSchema;
import it.datagen.prototype.model.rules.DataQualityRule;

import java.util.List;
import java.util.Objects;

/**
 * Validates a full dataset against a list of data quality rules.
 */
public class DatasetValidator {

	private final RuleProcessorRegistry ruleProcessors;

	public DatasetValidator() {
		this(RuleProcessorRegistry.withDefaults());
	}

	public DatasetValidator(RuleProcessorRegistry ruleProcessors) {
		this.ruleProcessors = ruleProcessors;
	}

	/**
	 * @return true if all rows satisfy all rules, false otherwise
	 */
	public boolean validate(TableSchema schema, List<Row> rows, List<DataQualityRule> rules) {
		Objects.requireNonNull(schema, "Table schema cannot be null.");
		Objects.requireNonNull(rows, "Rows cannot be null.");
		Objects.requireNonNull(rules, "Rules cannot be null.");

		// Shared context keeps stateful validators (e.g., unique keys) coherent.
		RuleProcessingContext context = new RuleProcessingContext();
		for (DataQualityRule rule : rules) {
			for (Row row : rows) {
				if (!ruleProcessors.validate(rule, row, rows, context)) {
					return false;
				}
			}
		}

		return true;
	}
}
