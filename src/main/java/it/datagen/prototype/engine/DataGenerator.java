package it.datagen.prototype.engine;

import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.engine.processing.RuleProcessorRegistry;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.DataQualityRule;

import java.util.List;

/**
 * Facade for rule-oriented dataset generation operations.
 */
public class DataGenerator {

	private final RuleProcessorRegistry ruleProcessors;

	public DataGenerator() {
		this(RuleProcessorRegistry.withDefaults());
	}

	public DataGenerator(RuleProcessorRegistry ruleProcessors) {
		this.ruleProcessors = ruleProcessors;
	}

	public boolean validateRule(DataQualityRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		return ruleProcessors.validate(rule, row, allRows, context);
	}

	public boolean fixRule(DataQualityRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		return ruleProcessors.fix(rule, row, allRows, context);
	}
}
