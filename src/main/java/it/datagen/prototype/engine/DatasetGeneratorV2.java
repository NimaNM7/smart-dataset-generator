package it.datagen.prototype.engine;

import it.datagen.prototype.engine.dependencies.DependencySolver;
import it.datagen.prototype.engine.generation.TypeValueGenerator;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.engine.processing.RuleProcessorRegistry;
import it.datagen.prototype.model.Column;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.TableSchema;
import it.datagen.prototype.model.rules.ColumnComparisonRule;
import it.datagen.prototype.model.rules.ConditionalCompletenessRule;
import it.datagen.prototype.model.rules.DataQualityRule;
import it.datagen.prototype.model.rules.FunctionalDependencyRule;
import it.datagen.prototype.model.rules.LengthComparisonRule;
import it.datagen.prototype.model.rules.TemporalOrderRule;

import java.util.ArrayList;
import java.util.Arrays;
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

	public DatasetGeneratorV2() {
		this(RuleProcessorRegistry.withDefaults(), new DependencySolver(), new TypeValueGenerator(), 3);
	}

	public DatasetGeneratorV2(RuleProcessorRegistry ruleProcessors, DependencySolver dependencySolver,
			TypeValueGenerator typeValueGenerator, int maxFixPasses) {
		this.ruleProcessors = Objects.requireNonNull(ruleProcessors, "Rule processors cannot be null.");
		this.dependencySolver = Objects.requireNonNull(dependencySolver, "Dependency solver cannot be null.");
		this.typeValueGenerator = Objects.requireNonNull(typeValueGenerator, "Type value generator cannot be null.");
		this.maxFixPasses = Math.max(1, maxFixPasses);
	}

	public List<Row> generateTable(TableSchema schema, List<DataQualityRule> rules, int rowCount) {
		Objects.requireNonNull(schema, "Table schema cannot be null.");
		Objects.requireNonNull(rules, "Rules cannot be null.");
		if (rowCount < 0) {
			throw new IllegalArgumentException("Row count must be >= 0.");
		}

		List<Set<String>> generationStages = buildGenerationStages(schema, rules);
		Map<String, Column> columnsByName = schema.getColumnMap();
		List<DataQualityRule> generationRules = List.copyOf(rules);
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
				generateStageValues(stageColumns, columnsByName, rowValues);
				availableColumns.addAll(stageColumns);
				if (!applyRulesUntilStable(generationRules, rowValues, allRowValues, context, availableColumns)) {
					throw new IllegalStateException("Unable to satisfy stage rules for row index " + rowIndex);
				}
			}

			if (!applyRulesUntilStable(generationRules, rowValues, allRowValues, context, allColumnNames)) {
				throw new IllegalStateException("Unable to satisfy row rules for row index " + rowIndex);
			}

			Row row = new Row(rowValues);
			generatedRows.add(row);
			allRowValues.add(row.getValues());
		}

		return generatedRows;
	}

	private List<Set<String>> buildGenerationStages(TableSchema schema, List<DataQualityRule> rules) {
		List<Set<String>> ruleDrivenStages = dependencySolver.solveTopologically(rules).stream()
				.filter(component -> !component.isEmpty())
				.map(LinkedHashSet::new)
				.collect(Collectors.toList());

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

	private void generateStageValues(Set<String> stageColumns, Map<String, Column> columnsByName,
			Map<String, Object> rowValues) {
		stageColumns.stream()
				.filter(columnName -> !rowValues.containsKey(columnName))
				.forEach(columnName -> {
					Column column = columnsByName.get(columnName);
					if (column == null) {
						throw new IllegalArgumentException("Column referenced by rule not found in schema: " + columnName);
					}
					rowValues.put(columnName, typeValueGenerator.generateValue(column.getType()));
				});
	}

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
	 * This function tells us weather we have enough data to validate the rule or not. We
	 * first get the columns that are important for our rule and then check that we generated
	 * these columns before or not.
	 */
	private boolean isEnoughDataForRuleValidation(DataQualityRule rule, Set<String> availableColumns) {
		Set<String> requiredColumns = requiredColumns(rule);
		return availableColumns.containsAll(requiredColumns);
	}

	private Set<String> requiredColumns(DataQualityRule rule) {
		if (rule instanceof ColumnComparisonRule comparisonRule) {
            return new LinkedHashSet<>(Arrays.asList(comparisonRule.getSmallerColumn(), comparisonRule.getLargerColumn()));
		}
		if (rule instanceof TemporalOrderRule temporalRule) {
            return new LinkedHashSet<>(Arrays.asList(temporalRule.getEarlierDateColumn(), temporalRule.getLaterDateColumn()));
		}
		if (rule instanceof LengthComparisonRule lengthRule) {
            return new LinkedHashSet<>(Arrays.asList(lengthRule.getFirstColumn(), lengthRule.getSecondColumn()));
		}
		if (rule instanceof ConditionalCompletenessRule completenessRule) {
            return new LinkedHashSet<>(Arrays.asList(completenessRule.getConditionColumn(), completenessRule.getTargetColumn()));
		}
		if (rule instanceof FunctionalDependencyRule functionalDependencyRule) {
            Set<String> columns = new LinkedHashSet<>(functionalDependencyRule.getDeterminantColumns());
			columns.add(functionalDependencyRule.getDependentColumn());
			return columns;
		}
		return Collections.emptySet();
	}
}
