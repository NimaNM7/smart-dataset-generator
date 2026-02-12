package it.datagen.prototype.model.rules;
import java.util.Objects;

/**
 * Represents an intra-table value comparison between a parent and a child row
 * (Rule #4.3). It uses a ColumnComparisonRule to define the logic of the
 * comparison.
 *
 * Example: The 'EstablishmentDate' of a child unit must be after or equal to
 * the 'EstablishmentDate' of its parent unit. Example: The 'Budget' of a
 * sub-project must be less than or equal to the 'Budget' of its parent project.
 */
public class ParentChildComparisonRule implements DataQualityRule {

	private final IntraTableParentRule parentRelationship;
	private final ColumnComparisonRule comparisonLogic;

	/**
	 * Constructs a ParentChildComparisonRule.
	 *
	 * @param parentRelationship The parent-child relationship definition.
	 * @param comparisonLogic    The rule defining how a child's column compares to
	 *                           a parent's column. The 'smallerColumn' is
	 *                           interpreted as the child's value constraint, and
	 *                           'largerColumn' as the parent's value.
	 */
	public ParentChildComparisonRule(IntraTableParentRule parentRelationship, ColumnComparisonRule comparisonLogic) {
		Objects.requireNonNull(parentRelationship, "Parent relationship cannot be null.");
		Objects.requireNonNull(comparisonLogic, "Comparison logic cannot be null.");

		this.parentRelationship = parentRelationship;
		this.comparisonLogic = comparisonLogic;
	}

	public IntraTableParentRule getParentRelationship() {
		return parentRelationship;
	}

	public ColumnComparisonRule getComparisonLogic() {
		return comparisonLogic;
	}
}
