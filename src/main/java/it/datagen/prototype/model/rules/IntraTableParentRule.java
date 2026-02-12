package it.datagen.prototype.model.rules;

import java.util.Set;

/**
 * Represents an intra-table reference rule (Rule #4), defining a parent-child
 * relationship. Example: In an employee table, a "Manager_ID" column refers to
 * another "Employee_ID" in the same table.
 */
public class IntraTableParentRule implements DataQualityRule {

	private final String childIdColumn;
	private final String parentIdColumn;

	/**
	 * @param childIdColumn  The name of the column acting as the row's primary
	 *                       identifier (e.g., "Employee_ID").
	 * @param parentIdColumn The name of the column containing the reference to the
	 *                       parent's ID (e.g., "Manager_ID").
	 */
	public IntraTableParentRule(String childIdColumn, String parentIdColumn) {
		if (childIdColumn == null || parentIdColumn == null || childIdColumn.trim().isEmpty()
				|| parentIdColumn.trim().isEmpty()) {
			throw new IllegalArgumentException("Column names cannot be null or empty.");
		}
		if (childIdColumn.equals(parentIdColumn)) {
			throw new IllegalArgumentException("Child ID and Parent ID columns cannot be the same.");
		}
		this.childIdColumn = childIdColumn;
		this.parentIdColumn = parentIdColumn;
	}

	public String getChildIdColumn() {
		return childIdColumn;
	}

	public String getParentIdColumn() {
		return parentIdColumn;
	}

	@Override
	public Set<String> requiredColumns() {
		return Set.of(childIdColumn, parentIdColumn);
	}
}
