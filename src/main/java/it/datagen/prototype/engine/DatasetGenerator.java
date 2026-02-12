package it.datagen.prototype.engine;

import it.datagen.prototype.engine.generation.TypeValueGenerator;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.engine.processing.RuleProcessorRegistry;
import it.datagen.prototype.model.Column;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.TableSchema;
import it.datagen.prototype.model.rules.DataQualityRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Facade for rule-oriented dataset generation operations.
 */
public class DatasetGenerator {

	private final RuleProcessorRegistry ruleProcessors;
	private final TypeValueGenerator typeValueGenerator;

	public DatasetGenerator() {
		this(RuleProcessorRegistry.withDefaults(), new TypeValueGenerator());
	}

	public DatasetGenerator(RuleProcessorRegistry ruleProcessors) {
		this(ruleProcessors, new TypeValueGenerator());
	}

	public DatasetGenerator(RuleProcessorRegistry ruleProcessors, TypeValueGenerator typeValueGenerator) {
		this.ruleProcessors = ruleProcessors;
		this.typeValueGenerator = typeValueGenerator;
	}

	public boolean validateRule(DataQualityRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		return ruleProcessors.validate(rule, row, allRows, context);
	}

	public boolean validateRule(DataQualityRule rule, Map<String, Object> rowValues, List<Map<String, Object>> allRows,
			RuleProcessingContext context) {
		return ruleProcessors.validate(rule, rowValues, allRows, context);
	}

	public boolean fixRule(DataQualityRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		return fixRule(rule, row, allRows, context, Collections.emptySet());
	}

	public boolean fixRule(DataQualityRule rule, Row row, List<Row> allRows, RuleProcessingContext context,
			Set<String> fixedColumns) {
		return ruleProcessors.fix(rule, row, allRows, context, fixedColumns);
	}

	public boolean fixRule(DataQualityRule rule, Map<String, Object> rowValues, List<Map<String, Object>> allRows,
			RuleProcessingContext context) {
		return fixRule(rule, rowValues, allRows, context, Collections.emptySet());
	}

	public boolean fixRule(DataQualityRule rule, Map<String, Object> rowValues, List<Map<String, Object>> allRows,
			RuleProcessingContext context, Set<String> fixedColumns) {
		return ruleProcessors.fix(rule, rowValues, allRows, context, fixedColumns);
	}

	public List<Row> generateTable(TableSchema tableSchema, List<DataQualityRule> rules) {
		Objects.requireNonNull(tableSchema, "Table schema cannot be null.");
		List<Row> allRows = new ArrayList<>();
		RuleProcessingContext context = new RuleProcessingContext();

		// TODO: do something else for these constants (const vals or env variables)
		for (int i = 0; i < 1000; i++) {
			Row row = generateRow(tableSchema.getColumns());
			int numberOfTries = 0;
			while (numberOfTries < 3) {
				if (!validateAndFixRow(rules, row, allRows, context))
					break;
				numberOfTries++;
			}
		}

		return allRows;
	}

	private Row generateRow(List<Column> columns) {
		Row row = new Row();
		for (Column column : columns) {
			Object value = typeValueGenerator.generateValue(column.getType());
			row.setValue(column.getName(), value);
		}
		return row;
	}

	private boolean validateAndFixRow(List<DataQualityRule> rules, Row row, List<Row> allRows, RuleProcessingContext context) {
		boolean hasProblem = false;
		for (DataQualityRule rule : rules) {
			boolean isValid = ruleProcessors.validate(rule, row, allRows, context);
			if (!isValid) {
				hasProblem = true;
				ruleProcessors.fix(rule, row, allRows, context);
			}
		}
		return hasProblem;
	}
}
