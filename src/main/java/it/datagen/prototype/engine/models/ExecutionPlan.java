package it.datagen.prototype.engine.models;

import it.datagen.prototype.model.rules.DataQualityRule;
import it.datagen.prototype.model.rules.RuleCategory;
import it.datagen.prototype.engine.constraints.GenerationConstraint;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExecutionPlan {

	private final List<ColumnMetadata> columns;
	private final List<Set<String>> columnComponentsInOrder;
	private final Map<DataQualityRule, RuleCategory> ruleCategories;
	private final Map<String, List<GenerationConstraint>> constraintIndex;
	private final List<GenerationConstraint> constraints;

	public ExecutionPlan(List<ColumnMetadata> columns,
			List<Set<String>> columnComponentsInOrder,
			Map<DataQualityRule, RuleCategory> ruleCategories,
			List<GenerationConstraint> constraints,
			Map<String, List<GenerationConstraint>> constraintIndex) {
		this.columns = List.copyOf(Objects.requireNonNull(columns, "columns"));
		this.columnComponentsInOrder = List.copyOf(Objects.requireNonNull(columnComponentsInOrder, "columnComponentsInOrder"));
		this.ruleCategories = Map.copyOf(Objects.requireNonNull(ruleCategories, "ruleCategories"));
		this.constraints = List.copyOf(Objects.requireNonNull(constraints, "constraints"));
		this.constraintIndex = Map.copyOf(Objects.requireNonNull(constraintIndex, "constraintIndex"));
	}

	public List<ColumnMetadata> getColumns() {
		return columns;
	}

	/**
	 * Topologically ordered SCC components of the column dependency graph.
	 *
	 * <p>Singleton sets represent acyclic columns.
	 */
	public List<Set<String>> getColumnComponentsInOrder() {
		return columnComponentsInOrder;
	}

	public Map<DataQualityRule, RuleCategory> getRuleCategories() {
		return ruleCategories;
	}

	public List<GenerationConstraint> getConstraints() {
		return constraints;
	}

	public Map<String, List<GenerationConstraint>> getConstraintIndex() {
		return constraintIndex;
	}

	public List<DataQualityRule> rulesByCategory(RuleCategory category) {
		if (category == null) {
			return Collections.emptyList();
		}
		return ruleCategories.entrySet().stream()
				.filter(entry -> entry.getValue() == category)
				.map(Map.Entry::getKey)
				.toList();
	}
}
