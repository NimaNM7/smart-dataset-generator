package it.datagen.prototype.model.rules;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents an intra-table equality rule for "sibling" rows (Rule #4.2). This
 * rule states that if two rows share the same parent (based on an
 * IntraTableParentRule), then their values in a specified target column must be
 * identical.
 *
 * Example: If two employees have the same 'Manager_ID', they must have the same
 * 'Department'.
 */
public class SiblingEqualityRule implements DataQualityRule {

	private final IntraTableParentRule parentRelationship;
	private final String targetColumn;

	/**
	 * Constructs a SiblingEqualityRule.
	 *
	 * @param parentRelationship The parent-child relationship rule that defines
	 *                           siblings.
	 * @param targetColumn       The column whose value must be the same for all
	 *                           siblings.
	 */
	public SiblingEqualityRule(IntraTableParentRule parentRelationship, String targetColumn) {
		Objects.requireNonNull(parentRelationship, "Parent relationship cannot be null.");
		Objects.requireNonNull(targetColumn, "Target column name cannot be null.");

		if (targetColumn.trim().isEmpty()) {
			throw new IllegalArgumentException("Target column name cannot be blank.");
		}
		// A sibling's common value cannot be the ID of the parent or the child.
		if (targetColumn.equals(parentRelationship.getParentIdColumn())
				|| targetColumn.equals(parentRelationship.getChildIdColumn())) {
			throw new IllegalArgumentException("Target column cannot be one of the relationship key columns.");
		}

		this.parentRelationship = parentRelationship;
		this.targetColumn = targetColumn;
	}

	public IntraTableParentRule getParentRelationship() {
		return parentRelationship;
	}

	public String getTargetColumn() {
		return targetColumn;
	}

	@Override
	public Set<String> requiredColumns() {
		Set<String> columns = new LinkedHashSet<>(parentRelationship.requiredColumns());
		columns.add(targetColumn);
		return Set.copyOf(columns);
	}
}
