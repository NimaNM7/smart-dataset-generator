package it.datagen.prototype.engine.validation;

import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.AggregationRule;
import it.datagen.prototype.model.rules.ColumnComparisonRule;
import it.datagen.prototype.model.rules.ConditionalCompletenessRule;
import it.datagen.prototype.model.rules.DataQualityRule;
import it.datagen.prototype.model.rules.FunctionalDependencyRule;
import it.datagen.prototype.model.rules.IntraTableParentRule;
import it.datagen.prototype.model.rules.LengthComparisonRule;
import it.datagen.prototype.model.rules.ParentChildComparisonRule;
import it.datagen.prototype.model.rules.PatternRule;
import it.datagen.prototype.model.rules.SiblingEqualityRule;
import it.datagen.prototype.model.rules.TemporalOrderRule;
import it.datagen.prototype.model.rules.UniqueKeyRule;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class RuleValidationVisitor {

	public boolean visit(ColumnComparisonRule rule, Row row, List<Row> allRows) {
		Integer comparison = compareValues(
				getValue(row, rule.getSmallerColumn()),
				getValue(row, rule.getLargerColumn())
		);
		if (comparison == null) {
			return false;
		}
		return rule.isAllowEqual() ? comparison <= 0 : comparison < 0;
	}

	public boolean visit(TemporalOrderRule rule, Row row, List<Row> allRows) {
		Integer comparison = compareValues(
				getValue(row, rule.getEarlierDateColumn()),
				getValue(row, rule.getLaterDateColumn())
		);
		if (comparison == null) {
			return false;
		}
		return rule.isAllowEqual() ? comparison <= 0 : comparison < 0;
	}

	public boolean visit(LengthComparisonRule rule, Row row, List<Row> allRows) {
		Object first = getValue(row, rule.getFirstColumn());
		Object second = getValue(row, rule.getSecondColumn());
		if (!(first instanceof CharSequence) || !(second instanceof CharSequence)) {
			return false;
		}

		int firstLength = ((CharSequence) first).length();
		int secondLength = ((CharSequence) second).length();

        return switch (rule.getComparison()) {
            case SHORTER_THAN_OR_EQUAL -> firstLength <= secondLength;
            case LONGER_THAN_OR_EQUAL -> firstLength >= secondLength;
            case STRICTLY_SHORTER_THAN -> firstLength < secondLength;
            case STRICTLY_LONGER_THAN -> firstLength > secondLength;
        };
	}

	public boolean visit(ConditionalCompletenessRule rule, Row row, List<Row> allRows) {
		Object conditionValue = getValue(row, rule.getConditionColumn());
		if (!Objects.equals(conditionValue, rule.getConditionValue())) {
			return true;
		}

		Object targetValue = getValue(row, rule.getTargetColumn());
		return rule.isTargetMustBeNull() == (targetValue == null);
	}

	public boolean visit(FunctionalDependencyRule rule, Row row, List<Row> allRows) {
		if (row == null || allRows == null) {
			return false;
		}

		Object dependentValue = row.getValue(rule.getDependentColumn());
		for (Row candidate : allRows) {
			if (candidate == null || candidate == row) {
				continue;
			}

			if (!hasSameDeterminants(row, candidate, rule.getDeterminantColumns())) {
				continue;
			}

			Object candidateDependentValue = candidate.getValue(rule.getDependentColumn());
			if (!Objects.equals(dependentValue, candidateDependentValue)) {
				return false;
			}
		}

		return true;
	}

	// Deferred rules: intentionally treated as pass-through until implemented.
	public boolean visit(AggregationRule rule, Row row, List<Row> allRows) {
		return true;
	}

	public boolean visit(IntraTableParentRule rule, Row row, List<Row> allRows) {
		return true;
	}

	public boolean visit(ParentChildComparisonRule rule, Row row, List<Row> allRows) {
		return true;
	}

	public boolean visit(PatternRule rule, Row row, List<Row> allRows) {
		return true;
	}

	public boolean visit(SiblingEqualityRule rule, Row row, List<Row> allRows) {
		return true;
	}

	public boolean visit(UniqueKeyRule rule, Row row, List<Row> allRows) {
		return true;
	}

	private boolean hasSameDeterminants(Row firstRow, Row secondRow, List<String> determinantColumns) {
		for (String determinant : determinantColumns) {
			if (!Objects.equals(firstRow.getValue(determinant), secondRow.getValue(determinant))) {
				return false;
			}
		}
		return true;
	}

	private Object getValue(Row row, String columnName) {
		return row == null ? null : row.getValue(columnName);
	}

	private Integer compareValues(Object left, Object right) {
		if (left == null || right == null) {
			return null;
		}

		if (left instanceof Number && right instanceof Number) {
			BigDecimal leftNumber = new BigDecimal(left.toString());
			BigDecimal rightNumber = new BigDecimal(right.toString());
			return leftNumber.compareTo(rightNumber);
		}

		if (left instanceof Date && right instanceof Date) {
			return Long.compare(((Date) left).getTime(), ((Date) right).getTime());
		}

		if (left instanceof Comparable && left.getClass().isInstance(right)) {
			@SuppressWarnings("unchecked")
			Comparable<Object> comparableLeft = (Comparable<Object>) left;
			return comparableLeft.compareTo(right);
		}

		return null;
	}
}
