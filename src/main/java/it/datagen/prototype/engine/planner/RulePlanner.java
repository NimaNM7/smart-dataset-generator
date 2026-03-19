package it.datagen.prototype.engine.planner;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.dependencies.DependencySolver;
import it.datagen.prototype.engine.models.ColumnMetadata;
import it.datagen.prototype.engine.models.ExecutionPlan;
import it.datagen.prototype.model.Column;
import it.datagen.prototype.model.rules.AggregationRule;
import it.datagen.prototype.model.rules.ColumnComparisonRule;
import it.datagen.prototype.model.rules.ConditionalCompletenessRule;
import it.datagen.prototype.model.rules.DataQualityRule;
import it.datagen.prototype.model.rules.FunctionalDependencyRule;
import it.datagen.prototype.model.rules.IntraTableParentRule;
import it.datagen.prototype.model.rules.LengthComparisonRule;
import it.datagen.prototype.model.rules.ParentChildComparisonRule;
import it.datagen.prototype.model.rules.PatternRule;
import it.datagen.prototype.model.rules.RuleCategory;
import it.datagen.prototype.model.rules.SiblingEqualityRule;
import it.datagen.prototype.model.rules.TemporalOrderRule;
import it.datagen.prototype.model.rules.UniqueKeyRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

			List<GenerationConstraint> constraints = compileConstraints(rules);
			Map<String, List<GenerationConstraint>> constraintIndex = buildConstraintIndex(constraints);

			return new ExecutionPlan(columnMetadata, components, categories, constraints, constraintIndex);
		}

	private Map<String, List<GenerationConstraint>> buildConstraintIndex(List<GenerationConstraint> constraints) {
		Map<String, List<GenerationConstraint>> index = new HashMap<>();
		for (GenerationConstraint constraint : constraints) {
			for (String column : constraint.involvedColumns()) {
				index.computeIfAbsent(column, ignored -> new ArrayList<>()).add(constraint);
			}
		}
		return index;
	}

	private List<GenerationConstraint> compileConstraints(List<DataQualityRule> rules) {
		List<GenerationConstraint> constraints = new ArrayList<>();
		for (DataQualityRule rule : rules) {
			// Row rules (intra-row domain narrowing)
			if (rule instanceof ColumnComparisonRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.RelationalConstraints.leq(
						r.getSmallerColumn(), r.getLargerColumn(), r.isAllowEqual()));
			} else if (rule instanceof TemporalOrderRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.RelationalConstraints.leq(
						r.getEarlierDateColumn(), r.getLaterDateColumn(), r.isAllowEqual()));
			} else if (rule instanceof LengthComparisonRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.StringLengthConstraints.fromRule(r));
			} else if (rule instanceof ConditionalCompletenessRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.ConditionalNullabilityConstraint.fromRule(r));
			}

			// Identity (context-backed)
			else if (rule instanceof FunctionalDependencyRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.FunctionalDependencyConstraint.fromRule(r));
			} else if (rule instanceof UniqueKeyRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.UniqueKeyConstraint.fromRule(r));
			}

			// Structural (context-backed)
			else if (rule instanceof IntraTableParentRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.IntraTableReferenceConstraint.fromRule(r));
			}

			// Global/statistical (context-backed)
			else if (rule instanceof AggregationRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.AggregationAllowanceConstraint.fromRule(r));
			}

			// Cell rules (single-column pattern enforcement)
			else if (rule instanceof PatternRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.PatternConstraint.fromRule(r));
			}

			// Structural (context-backed sibling equality)
			else if (rule instanceof SiblingEqualityRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.SiblingEqualityConstraint.fromRule(r));
			}

			// Structural (context-backed cross-row comparison)
			else if (rule instanceof ParentChildComparisonRule r) {
				constraints.add(it.datagen.prototype.engine.constraints.builtin.ParentChildComparisonConstraint.fromRule(r));
			}
		}
		return constraints;
	}
}
