package it.datagen.prototype.model.rules;

import java.util.Objects;
import java.util.Set;

/**
 * Represents a rule that compares values between two columns within the same
 * row. This is a generic version of rules like TemporalOrderRule or
 * AmountComparisonRule. It ensures that the value in one column is smaller
 * than, or equal to, the value in another.
 *
 * Example (Temporal): 'StartDate' must be before or equal to 'EndDate'. Example
 * (Amount): 'DiscountedPrice' must be less than or equal to 'OriginalPrice'.
 */
public class ColumnComparisonRule implements DataQualityRule {

	private final String smallerColumn;
	private final String largerColumn;
	private final boolean allowEqual;

	/**
	 * Constructs a ColumnComparisonRule.
	 *
	 * @param smallerColumn The name of the column that should contain the smaller
	 *                      (or earlier) value.
	 * @param largerColumn  The name of the column that should contain the larger
	 *                      (or later) value.
	 * @param allowEqual    If true, the values can be equal. If false, the
	 *                      'smaller' must be strictly smaller.
	 */
	public ColumnComparisonRule(String smallerColumn, String largerColumn, boolean allowEqual) {
		Objects.requireNonNull(smallerColumn, "Smaller column name cannot be null.");
		Objects.requireNonNull(largerColumn, "Larger column name cannot be null.");

		if (smallerColumn.trim().isEmpty() || largerColumn.trim().isEmpty()) {
			throw new IllegalArgumentException("Column names cannot be blank.");
		}
		if (smallerColumn.equals(largerColumn)) {
			throw new IllegalArgumentException("Smaller and larger columns cannot be the same.");
		}

		this.smallerColumn = smallerColumn;
		this.largerColumn = largerColumn;
		this.allowEqual = allowEqual;
	}

	public String getSmallerColumn() {
		return smallerColumn;
	}

	public String getLargerColumn() {
		return largerColumn;
	}

	public boolean isAllowEqual() {
		return allowEqual;
	}

	@Override
	public Set<String> requiredColumns() {
		return Set.of(smallerColumn, largerColumn);
	}

	@Override
	public RuleCategory category() {
		return RuleCategory.ROW;
	}

}
