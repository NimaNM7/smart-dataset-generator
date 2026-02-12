package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.rules.FunctionalDependencyRule;

import java.util.ArrayList;
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
		if (rule == null || rowValues == null || allRowValues == null) {
			return false;
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
		if (rule == null || rowValues == null || allRowValues == null) {
			return false;
		}
		if (fixedColumns.contains(rule.getDependentColumn())) {
			return false;
		}

		List<Map<String, Object>> equivalenceClass = new ArrayList<>();
		equivalenceClass.add(rowValues);
		for (Map<String, Object> candidate : allRowValues) {
			if (candidate == null || candidate == rowValues) {
				continue;
			}
			if (sameDeterminants(rowValues, candidate, rule.getDeterminantColumns())) {
				equivalenceClass.add(candidate);
			}
		}

		Object canonicalValue = selectCanonicalDependentValue(rule, equivalenceClass);
		rowValues.put(rule.getDependentColumn(), canonicalValue);
		return true;
	}

	private Object selectCanonicalDependentValue(FunctionalDependencyRule rule, List<Map<String, Object>> rows) {
		for (Map<String, Object> candidate : rows) {
			Object value = candidate.get(rule.getDependentColumn());
			if (value != null) {
				return value;
			}
		}
		return null;
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
