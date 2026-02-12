package it.datagen.prototype.model.rules;

/**
 * Represents a temporal order rule (Rule #1). Ensures that a date in one column
 * is chronologically before (or equal to) a date in another column.
 */
public class TemporalOrderRule implements DataQualityRule {

	private final String earlierDateColumn;
	private final String laterDateColumn;
	private final boolean allowEqual;

	/**
	 * @param earlierDateColumn The name of the column containing the earlier date.
	 * @param laterDateColumn   The name of the column containing the later date.
	 * @param allowEqual        If true, dates can be equal (<=). If false, the
	 *                          earlier date must be strictly smaller (<).
	 */
	public TemporalOrderRule(String earlierDateColumn, String laterDateColumn, boolean allowEqual) {
		if (earlierDateColumn == null || laterDateColumn == null || earlierDateColumn.trim().isEmpty()
				|| laterDateColumn.trim().isEmpty()) {
			throw new IllegalArgumentException("Column names cannot be null or empty.");
		}
		if (earlierDateColumn.equals(laterDateColumn)) {
			throw new IllegalArgumentException("Earlier and later date columns cannot be the same.");
		}
		this.earlierDateColumn = earlierDateColumn;
		this.laterDateColumn = laterDateColumn;
		this.allowEqual = allowEqual;
	}

	public String getEarlierDateColumn() {
		return earlierDateColumn;
	}

	public String getLaterDateColumn() {
		return laterDateColumn;
	}

	public boolean isAllowEqual() {
		return allowEqual;
	}

}
