package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.FunctionalDependencyRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FunctionalDependencyRuleHandler implements RuleHandler<FunctionalDependencyRule> {

	@Override
	public Class<FunctionalDependencyRule> ruleType() {
		return FunctionalDependencyRule.class;
	}

	@Override
	public boolean validate(FunctionalDependencyRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (rule == null || row == null || allRows == null) {
			return false;
		}

		Object dependentValue = row.getValue(rule.getDependentColumn());
		for (Row candidate : allRows) {
			if (candidate == null || candidate == row) {
				continue;
			}

			if (!sameDeterminants(row, candidate, rule.getDeterminantColumns())) {
				continue;
			}

			Object candidateDependentValue = candidate.getValue(rule.getDependentColumn());
			if (!Objects.equals(dependentValue, candidateDependentValue)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean fix(FunctionalDependencyRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (validate(rule, row, allRows, context)) {
			return true;
		}
		if (rule == null || row == null || allRows == null) {
			return false;
		}

		List<Row> equivalenceClass = new ArrayList<>();
		equivalenceClass.add(row);
		for (Row candidate : allRows) {
			if (candidate == null || candidate == row) {
				continue;
			}
			if (sameDeterminants(row, candidate, rule.getDeterminantColumns())) {
				equivalenceClass.add(candidate);
			}
		}

		Object canonicalValue = selectCanonicalDependentValue(rule, equivalenceClass);
		row.setValue(rule.getDependentColumn(), canonicalValue);
		return true;
	}

	private Object selectCanonicalDependentValue(FunctionalDependencyRule rule, List<Row> rows) {
		for (Row candidate : rows) {
			Object value = candidate.getValue(rule.getDependentColumn());
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private boolean sameDeterminants(Row first, Row second, List<String> determinantColumns) {
		for (String determinantColumn : determinantColumns) {
			Object leftValue = first.getValue(determinantColumn);
			Object rightValue = second.getValue(determinantColumn);
			if (!Objects.equals(leftValue, rightValue)) {
				return false;
			}
		}
		return true;
	}
}
