package it.datagen.prototype.engine;

import it.datagen.prototype.engine.dependencies.DependencySolver;
import it.datagen.prototype.engine.generation.TypeValueGenerator;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.engine.processing.RuleProcessorRegistry;
import it.datagen.prototype.model.Column;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.TableSchema;
import it.datagen.prototype.model.rules.DataQualityRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dataset generator based on a staged, dependency-aware execution strategy.
 * <p>
 * Strategy:
 * 1) Build a generation plan from SCCs in topological order
 * 2) Generate column values stage by stage
 * 3) Apply validate/fix tools on currently available columns
 * 4) Finalize the row once all intra-row rules are stable
 */
public class DatasetGeneratorV2 {

	private final RuleProcessorRegistry ruleProcessors;
	private final DependencySolver dependencySolver;
	private final TypeValueGenerator typeValueGenerator;
	private final int maxFixPasses;

	/**
	 * Creates a generator with default collaborators.
	 */
	public DatasetGeneratorV2() {
		this(RuleProcessorRegistry.withDefaults(), new DependencySolver(), new TypeValueGenerator(), 3);
	}

	/**
	 * Creates a generator with explicit collaborators.
	 *
	 * @param ruleProcessors    rule validation/fix registry
	 * @param dependencySolver  dependency solver used for staged ordering
	 * @param typeValueGenerator column type value generator
	 * @param maxFixPasses      maximum validate/fix passes per stage
	 */
	public DatasetGeneratorV2(RuleProcessorRegistry ruleProcessors, DependencySolver dependencySolver,
			TypeValueGenerator typeValueGenerator, int maxFixPasses) {
		this.ruleProcessors = Objects.requireNonNull(ruleProcessors, "Rule processors cannot be null.");
		this.dependencySolver = Objects.requireNonNull(dependencySolver, "Dependency solver cannot be null.");
		this.typeValueGenerator = Objects.requireNonNull(typeValueGenerator, "Type value generator cannot be null.");
		this.maxFixPasses = Math.max(1, maxFixPasses);
	}

	/**
	 * Generates a dataset using staged intra-row validation/fix passes.
	 *
	 * @param schema   table schema
	 * @param rules    quality rules used during generation
	 * @param rowCount number of rows to generate
	 * @return generated rows
	 */
	public List<Row> generateTable(TableSchema schema, List<DataQualityRule> rules, int rowCount) {
		Objects.requireNonNull(schema, "Table schema cannot be null.");
		Objects.requireNonNull(rules, "Rules cannot be null.");
		if (rowCount < 0) {
			throw new IllegalArgumentException("Row count must be >= 0.");
		}

		validateRuleColumnsExist(schema, rules);
		List<Set<String>> generationStages = buildGenerationStages(schema, rules);
		Set<String> allColumnNames = schema.getColumns().stream()
				.map(Column::getName)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		List<Row> generatedRows = new ArrayList<>(rowCount);
		List<Map<String, Object>> allRowValues = new ArrayList<>(rowCount);
		RuleProcessingContext context = new RuleProcessingContext();

		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
			Map<String, Object> rowValues = new HashMap<>();
			Set<String> availableColumns = new LinkedHashSet<>();

			for (Set<String> stageColumns : generationStages) {
				generateStageValues(stageColumns, schema.getColumnMap(), rowValues);
				availableColumns.addAll(stageColumns);
				if (!applyRulesUntilStable(rules, rowValues, allRowValues, context, availableColumns)) {
					throw new IllegalStateException("Unable to satisfy stage rules for row index " + rowIndex);
				}
			}

			if (!applyRulesUntilStable(rules, rowValues, allRowValues, context, allColumnNames)) {
				throw new IllegalStateException("Unable to satisfy row rules for row index " + rowIndex);
			}

			Row row = new Row(rowValues);
			generatedRows.add(row);
			allRowValues.add(row.getValues());
		}

		return generatedRows;
	}

	/**
	 * Builds topological SCC stages and appends singleton stages for unconstrained columns.
	 *
	 * @param schema table schema
	 * @param rules  rules used for dependency analysis
	 * @return ordered generation stages
	 */
	private List<Set<String>> buildGenerationStages(TableSchema schema, List<DataQualityRule> rules) {
		List<Set<String>> ruleDrivenStages = dependencySolver.solveTopologically(rules).stream()
				.<Set<String>>map(LinkedHashSet::new)
				.toList();

		Set<String> plannedColumns = ruleDrivenStages.stream()
				.flatMap(Set::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		List<Set<String>> unconstrainedStages = schema.getColumns().stream()
				.map(Column::getName)
				.filter(columnName -> !plannedColumns.contains(columnName))
				.map(Collections::singleton)
				.toList();

		return Stream.concat(ruleDrivenStages.stream(), unconstrainedStages.stream())
				.collect(Collectors.toList());
	}

	/**
	 * Generates all values for one stage.
	 *
	 * @param stageColumns stage columns
	 * @param columnsByName schema column index
	 * @param rowValues target row values map
	 */
	private void generateStageValues(Set<String> stageColumns, Map<String, Column> columnsByName,
			Map<String, Object> rowValues) {
		stageColumns.forEach(columnName -> rowValues.put(columnName,
				typeValueGenerator.generateValue(columnsByName.get(columnName).getType())));
	}

	/**
	 * Repeatedly validates and fixes applicable rules until stable or pass budget is exhausted.
	 *
	 * @param rules rules to process
	 * @param rowValues current row values
	 * @param allRowValues already generated rows
	 * @param context processing context
	 * @param availableColumns currently generated columns
	 * @return true when all applicable rules are valid
	 */
	private boolean applyRulesUntilStable(List<DataQualityRule> rules, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context, Set<String> availableColumns) {
		for (int pass = 0; pass < maxFixPasses; pass++) {
			boolean changed = false;
			for (DataQualityRule rule : rules) {
				if (!isEnoughDataForRuleValidation(rule, availableColumns)) {
					continue;
				}

				boolean valid = ruleProcessors.validate(rule, rowValues, allRowValues, context);
				if (valid) {
					continue;
				}

				boolean fixed = ruleProcessors.fix(rule, rowValues, allRowValues, context);
				if (!fixed) {
					return false;
				}
				changed = true;
			}

			if (!changed) {
				return true;
			}
		}

		for (DataQualityRule rule : rules) {
			if (!isEnoughDataForRuleValidation(rule, availableColumns)) {
				continue;
			}
			if (!ruleProcessors.validate(rule, rowValues, allRowValues, context)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true when all required columns for this rule are already available.
	 *
	 * @param rule rule to inspect
	 * @param availableColumns currently generated columns
	 * @return whether validation/fix can run at this stage
	 */
	private boolean isEnoughDataForRuleValidation(DataQualityRule rule, Set<String> availableColumns) {
		return availableColumns.containsAll(rule.requiredColumns());
	}

	/**
	 * Validates that all rule-referenced columns exist in the schema.
	 *
	 * @param schema table schema
	 * @param rules rules to verify
	 */
	private void validateRuleColumnsExist(TableSchema schema, List<DataQualityRule> rules) {
		Set<String> schemaColumns = schema.getColumns().stream()
				.map(Column::getName)
				.collect(Collectors.toSet());

		Set<String> unknownColumns = rules.stream()
				.map(DataQualityRule::requiredColumns)
				.flatMap(Set::stream)
				.filter(columnName -> !schemaColumns.contains(columnName))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		if (!unknownColumns.isEmpty()) {
			throw new IllegalArgumentException("Rules reference columns that do not exist in schema: " + unknownColumns);
		}
	}
}
