package it.datagen.prototype.model.rules;

import it.datagen.prototype.engine.dependencies.DependencyGraphVisitor;

import java.util.Objects;

/**
 * Represents a rule that compares the string length of values in two different
 * columns.
 *
 * Example: The length of 'Acronym' should not be greater than the length of
 * 'FullName'.
 */
public class LengthComparisonRule implements DataQualityRule, DependencyAwareRule {

	public enum ComparisonType {
		SHORTER_THAN_OR_EQUAL, LONGER_THAN_OR_EQUAL, STRICTLY_SHORTER_THAN, STRICTLY_LONGER_THAN
	}

	private final String firstColumn;
	private final String secondColumn;
	private final ComparisonType comparison;

	/**
	 * Constructs a LengthComparisonRule.
	 *
	 * @param firstColumn  The name of the first column for the comparison.
	 * @param secondColumn The name of the second column for the comparison.
	 * @param comparison   The type of length comparison to perform (e.g., first
	 *                     must be shorter than second).
	 */
	public LengthComparisonRule(String firstColumn, String secondColumn, ComparisonType comparison) {
		Objects.requireNonNull(firstColumn, "First column name cannot be null.");
		Objects.requireNonNull(secondColumn, "Second column name cannot be null.");
		Objects.requireNonNull(comparison, "ComparisonType cannot be null.");

		if (firstColumn.trim().isEmpty() || secondColumn.trim().isEmpty()) {
			throw new IllegalArgumentException("Column names cannot be blank.");
		}
		if (firstColumn.equals(secondColumn)) {
			throw new IllegalArgumentException("Columns for comparison cannot be the same.");
		}

		this.firstColumn = firstColumn;
		this.secondColumn = secondColumn;
		this.comparison = comparison;
	}

	public String getFirstColumn() {
		return firstColumn;
	}

	public String getSecondColumn() {
		return secondColumn;
	}

	public ComparisonType getComparison() {
		return comparison;
	}

	@Override
	public void accept(DependencyGraphVisitor visitor) {
		visitor.visit(this);
	}
}
