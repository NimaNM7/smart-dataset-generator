package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.models.RuleProcessingContext;
import it.datagen.prototype.model.rules.FunctionalDependencyRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FunctionalDependencyRuleHandler implements RuleHandler<FunctionalDependencyRule> {

	@Override
	public Class<FunctionalDependencyRule> ruleType() {
		return FunctionalDependencyRule.class;
	}

	@Override
	public boolean validateValues(FunctionalDependencyRule rule, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context) {
		if (rule == null || rowValues == null) {
			return false;
		}
		if (allRowValues == null || allRowValues.isEmpty()) {
			return true;
		}

		Object dependentValue = rowValues.get(rule.getDependentColumn());
		for (Map<String, Object> candidate : allRowValues) {
			if (candidate == null || candidate == rowValues) {
				continue;
			}

			if (!sameDeterminants(rowValues, candidate, rule.getDeterminantColumns())) {
				continue;
			}

			Object candidateDependentValue = candidate.get(rule.getDependentColumn());
			if (!Objects.equals(dependentValue, candidateDependentValue)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean fixValues(FunctionalDependencyRule rule, Map<String, Object> rowValues,
			List<Map<String, Object>> allRowValues, RuleProcessingContext context, Set<String> fixedColumns) {
		if (rule == null || rowValues == null) {
			return false;
		}
		Set<String> lockedColumns = fixedColumns == null ? Collections.emptySet() : fixedColumns;
		if (lockedColumns.contains(rule.getDependentColumn())) {
			return false;
		}
		if (allRowValues == null || allRowValues.isEmpty()) {
			return true;
		}

		for (Map<String, Object> candidate : allRowValues) {
			if (candidate == null || candidate == rowValues) {
				continue;
			}
			if (sameDeterminants(rowValues, candidate, rule.getDeterminantColumns())) {
				rowValues.put(rule.getDependentColumn(), candidate.get(rule.getDependentColumn()));
				return true;
			}
		}

		return false;
	}

	private boolean sameDeterminants(Map<String, Object> first, Map<String, Object> second,
			List<String> determinantColumns) {
		for (String determinantColumn : determinantColumns) {
			Object leftValue = first.get(determinantColumn);
			Object rightValue = second.get(determinantColumn);
			if (!Objects.equals(leftValue, rightValue)) {
				return false;
			}
		}
		return true;
	}
}
