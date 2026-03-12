package it.datagen.prototype.engine.models;

import it.datagen.prototype.model.rules.DataQualityRule;
import it.datagen.prototype.model.rules.RuleCategory;
import it.datagen.prototype.engine.planner.constraints.DomainConstraint;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExecutionPlan {

	private final List<ColumnMetadata> columns;
	private final List<Set<String>> columnComponentsInOrder;
	private final Map<DataQualityRule, RuleCategory> ruleCategories;
	private final Map<String, List<DomainConstraint>> constraintIndex;

	public ExecutionPlan(List<ColumnMetadata> columns,
			List<Set<String>> columnComponentsInOrder,
			Map<DataQualityRule, RuleCategory> ruleCategories,
			Map<String, List<DomainConstraint>> constraintIndex) {
		this.columns = List.copyOf(Objects.requireNonNull(columns, "columns"));
		this.columnComponentsInOrder = List.copyOf(Objects.requireNonNull(columnComponentsInOrder, "columnComponentsInOrder"));
		this.ruleCategories = Map.copyOf(Objects.requireNonNull(ruleCategories, "ruleCategories"));
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

	public Map<String, List<DomainConstraint>> getConstraintIndex() {
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
