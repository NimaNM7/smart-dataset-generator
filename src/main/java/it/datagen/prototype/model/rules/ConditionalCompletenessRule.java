package it.datagen.prototype.model.rules;

import java.util.Objects;

/**
 * Represents a conditional completeness rule (Implication Rule 7.1). This rule
 * enforces that if a certain condition is met in one column, another column
 * must either be non-null or null.
 *
 * Example: If 'Status' column is 'Completed', the 'CompletionDate' column must
 * not be empty. Example: If 'HasDiscount' column is false, the 'DiscountAmount'
 * column must be empty.
 */
public class ConditionalCompletenessRule implements DataQualityRule {

	private final String conditionColumn;
	private final Object conditionValue;
	private final String targetColumn;
	private final boolean targetMustBeNull; // If true, target must be null. If false, it must be non-null.

	/**
	 * Constructs a ConditionalCompletenessRule.
	 *
	 * @param conditionColumn  The name of the column that holds the condition.
	 * @param conditionValue   The value that triggers the rule.
	 * @param targetColumn     The name of the column whose completeness is checked.
	 * @param targetMustBeNull If true, the target must be null; if false, it must
	 *                         be non-null.
	 */
	public ConditionalCompletenessRule(String conditionColumn, Object conditionValue, String targetColumn,
			boolean targetMustBeNull) {
		Objects.requireNonNull(conditionColumn, "Condition column name cannot be null.");
		Objects.requireNonNull(conditionValue, "Condition value cannot be null.");
		Objects.requireNonNull(targetColumn, "Target column name cannot be null.");

		if (conditionColumn.trim().isEmpty() || targetColumn.trim().isEmpty()) {
			throw new IllegalArgumentException("Column names cannot be blank.");
		}
		if (conditionColumn.equals(targetColumn)) {
			throw new IllegalArgumentException("Condition and target columns cannot be the same.");
		}

		this.conditionColumn = conditionColumn;
		this.conditionValue = conditionValue;
		this.targetColumn = targetColumn;
		this.targetMustBeNull = targetMustBeNull;
	}

	public String getConditionColumn() {
		return conditionColumn;
	}

	public Object getConditionValue() {
		return conditionValue;
	}

	public String getTargetColumn() {
		return targetColumn;
	}

	public boolean isTargetMustBeNull() {
		return targetMustBeNull;
	}
}