package it.datagen.prototype.engine.planner;

import it.datagen.prototype.engine.dependencies.DependencySolver;
import it.datagen.prototype.engine.models.ColumnMetadata;
import it.datagen.prototype.engine.models.ExecutionPlan;
import it.datagen.prototype.engine.planner.constraints.ColumnComparisonConstraint;
import it.datagen.prototype.engine.planner.constraints.ConditionalCompletenessConstraint;
import it.datagen.prototype.engine.planner.constraints.DomainConstraint;
import it.datagen.prototype.engine.planner.constraints.LengthComparisonConstraint;
import it.datagen.prototype.engine.planner.constraints.TemporalOrderConstraint;
import it.datagen.prototype.model.Column;
import it.datagen.prototype.model.rules.ColumnComparisonRule;
import it.datagen.prototype.model.rules.ConditionalCompletenessRule;
import it.datagen.prototype.model.rules.DataQualityRule;
import it.datagen.prototype.model.rules.LengthComparisonRule;
import it.datagen.prototype.model.rules.RuleCategory;
import it.datagen.prototype.model.rules.TemporalOrderRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RulePlanner {

	private final DependencySolver dependencySolver;

	public RulePlanner() {
		this(new DependencySolver());
	}

	public RulePlanner(DependencySolver dependencySolver) {
		this.dependencySolver = Objects.requireNonNull(dependencySolver, "dependencySolver");
	}

	public ExecutionPlan plan(List<Column> columns, List<DataQualityRule> rules) {
		Objects.requireNonNull(columns, "columns");
		Objects.requireNonNull(rules, "rules");

		Set<String> schemaColumns = columns.stream()
				.map(Column::getName)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		Set<String> unknownColumns = rules.stream()
				.map(DataQualityRule::requiredColumns)
				.flatMap(Set::stream)
				.filter(columnName -> !schemaColumns.contains(columnName))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (!unknownColumns.isEmpty()) {
			throw new IllegalArgumentException("Rules reference columns that do not exist in schema: " + unknownColumns);
		}

		List<Set<String>> ruleDriven = dependencySolver.solveTopologically(rules).stream()
				.<Set<String>>map(LinkedHashSet::new)
				.toList();

		Set<String> planned = ruleDriven.stream()
				.flatMap(Set::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		List<Set<String>> unconstrained = columns.stream()
				.map(Column::getName)
				.filter(columnName -> !planned.contains(columnName))
				.map(Collections::singleton)
				.toList();

		List<Set<String>> components = new ArrayList<>(ruleDriven.size() + unconstrained.size());
		components.addAll(ruleDriven);
		components.addAll(unconstrained);

		Map<DataQualityRule, RuleCategory> categories = new HashMap<>();
		for (DataQualityRule rule : rules) {
			categories.put(rule, rule == null ? RuleCategory.UNKNOWN : rule.category());
		}

			List<ColumnMetadata> columnMetadata = columns.stream()
					.map(column -> new ColumnMetadata(column.getName(), column.getType()))
					.toList();

			Map<String, List<DomainConstraint>> constraintIndex = buildConstraintIndex(rules);

			return new ExecutionPlan(columnMetadata, components, categories, constraintIndex);
		}

		private Map<String, List<DomainConstraint>> buildConstraintIndex(List<DataQualityRule> rules) {
			Map<String, List<DomainConstraint>> index = new HashMap<>();
			for (DomainConstraint constraint : compileConstraints(rules)) {
				for (String column : constraint.involvedColumns()) {
					index.computeIfAbsent(column, ignored -> new LinkedList<>()).add(constraint);
				}
			}
			return index;
		}

		private List<DomainConstraint> compileConstraints(List<DataQualityRule> rules) {
			List<DomainConstraint> constraints = new ArrayList<>();
			for (DataQualityRule rule : rules) {
				if (rule instanceof ColumnComparisonRule r) {
					constraints.add(new ColumnComparisonConstraint(r));
				} else if (rule instanceof TemporalOrderRule r) {
					constraints.add(new TemporalOrderConstraint(r));
				} else if (rule instanceof LengthComparisonRule r) {
					constraints.add(new LengthComparisonConstraint(r));
				} else if (rule instanceof ConditionalCompletenessRule r) {
					constraints.add(new ConditionalCompletenessConstraint(r));
				}
			}
			return constraints;
		}
	}
