package it.datagen.prototype.model.rules;

import it.datagen.prototype.engine.validation.RuleValidationVisitor;
import it.datagen.prototype.model.Row;

import java.util.List;
import java.util.Objects;

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
	public boolean accept(RuleValidationVisitor visitor, Row row, List<Row> allRows) {
		return visitor.visit(this, row, allRows);
	}
}
