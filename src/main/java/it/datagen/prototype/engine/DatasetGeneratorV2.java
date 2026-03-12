package it.datagen.prototype.engine;

import it.datagen.prototype.engine.dependencies.DependencySolver;
import it.datagen.prototype.engine.generation.TypeValueGenerator;
import it.datagen.prototype.engine.models.RuleProcessingContext;
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
		this(RuleProcessorRegistry.withDefaults(), new DependencySolver(), new TypeValueGenerator(), 5);
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
		List<Set<String/*ColumnName*/>> generationStages = buildGenerationStages(schema, rules);
		Map<String/*ColumnName*/, Set<DataQualityRule>> rulesByColumn = buildRulesByColumn(rules);

		List<Row> generatedRows = new ArrayList<>(rowCount);
		List<Map<String, Object>> tableRowValues = new ArrayList<>(rowCount);
		RuleProcessingContext context = new RuleProcessingContext();

		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
			Map<String, Object> currentRowValues = new HashMap<>();
			Set<String> availableColumns = new LinkedHashSet<>();
			Set<String> fixedColumns = new LinkedHashSet<>();
			Set<DataQualityRule> rulesToCheck = new LinkedHashSet<>();

			for (Set<String> stageColumns : generationStages) {
				currentRowValues.putAll(generateStageInitialRowValues(stageColumns, schema.getColumnMap()));
				availableColumns.addAll(stageColumns);
				rulesToCheck.addAll(rulesToCheckForColumns(stageColumns, rulesByColumn));
				boolean foundStableRowValues = applyRulesUntilStable(
						rulesToCheck, currentRowValues, tableRowValues, context, availableColumns, fixedColumns
				);
				if (!foundStableRowValues) {
					throw new IllegalStateException("Unable to satisfy stage rules for row index " + rowIndex);
				}
				fixedColumns.addAll(stageColumns);
			}

			boolean foundStableRowValues = applyRulesUntilStable(
					rulesToCheck, currentRowValues, tableRowValues, context, availableColumns, fixedColumns
			);
			if (!foundStableRowValues) {
				throw new IllegalStateException("Unable to satisfy row rules for row index " + rowIndex);
			}

			Row row = new Row(currentRowValues);
			generatedRows.add(row);
			tableRowValues.add(row.getValues());
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
	 * Builds reverse index from column names to rules that depend on them.
	 *
	 * @param rules rules to index
	 * @return rules keyed by required column
	 */
	private Map<String, Set<DataQualityRule>> buildRulesByColumn(List<DataQualityRule> rules) {
		Map<String, Set<DataQualityRule>> rulesByColumn = new HashMap<>();
		for (DataQualityRule rule : rules) {
			for (String columnName : rule.requiredColumns()) {
				rulesByColumn.computeIfAbsent(columnName, ignored -> new LinkedHashSet<>()).add(rule);
			}
		}
		return rulesByColumn;
	}

	/**
	 * Generates all values for one stage.
	 *
	 * @param stageColumns stage columns
	 * @param columnsByName schema column index
	 * @return rowValues for this stage
	 */
	private Map<String, Object> generateStageInitialRowValues(Set<String> stageColumns, Map<String, Column> columnsByName) {
		return stageColumns.stream()
				.collect(Collectors.toMap(
						columnName -> columnName,
						columnName -> typeValueGenerator.generateValue(
								columnsByName.get(columnName).getType()
						)
				));
	}

	/**
	 * Returns rules that depend on any of the provided columns.
	 *
	 * @param changedColumns changed columns
	 * @param rulesByColumn lookup from column name to dependent rules
	 * @return rules to check
	 */
	private Set<DataQualityRule> rulesToCheckForColumns(Set<String> changedColumns,
			Map<String, Set<DataQualityRule>> rulesByColumn) {
		return changedColumns.stream()
				.map(rulesByColumn::get)
				.filter(Objects::nonNull)
				.flatMap(Set::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Repeatedly validates and fixes applicable rules-to-check until stable or pass budget is exhausted.
	 *
	 * @param rulesToCheck rules that must be (re)checked
	 * @param currentRowValues current row values
	 * @param tableRowValues already generated rows
	 * @param context processing context
	 * @param availableColumns currently generated columns
	 * @param fixedColumns columns from previous stages that fix methods must not modify
	 * @return check result with success flag and remaining rules to check
	 */
	private boolean applyRulesUntilStable(Set<DataQualityRule> rulesToCheck, Map<String, Object> currentRowValues,
			List<Map<String, Object>> tableRowValues, RuleProcessingContext context, Set<String> availableColumns,
			Set<String> fixedColumns) {

		// some rules must be checked later, because they check columns that we don't have now.
		Set<DataQualityRule> currentRulesToCheck = rulesToCheck.stream().filter(rule ->
				availableColumns.containsAll(rule.requiredColumns())).collect(Collectors.toSet());

		for (int pass = 0; pass < maxFixPasses; pass++) {
			if (currentRulesToCheck.isEmpty()) {
				return true;
			}

			boolean anyFixApplied = false;

			for (DataQualityRule rule : currentRulesToCheck) {

				boolean valid = ruleProcessors.validate(rule, currentRowValues, tableRowValues, context);
				if (valid) {
					continue;
				}

				boolean fixed = ruleProcessors.fix(rule, currentRowValues, tableRowValues, context, fixedColumns);
				if (!fixed) {
					return false;
				}

					anyFixApplied = true;
				}

				if (!anyFixApplied) {
					// all rules that were only for the columns of this stage are valid and there is no need to recheck them.
					rulesToCheck.removeAll(currentRulesToCheck);
					return true;
				}
			}

			for (DataQualityRule rule : currentRulesToCheck) {
				if (!ruleProcessors.validate(rule, currentRowValues, tableRowValues, context)) {
					return false;
				}
			}
			rulesToCheck.removeAll(currentRulesToCheck);
			return true;
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
