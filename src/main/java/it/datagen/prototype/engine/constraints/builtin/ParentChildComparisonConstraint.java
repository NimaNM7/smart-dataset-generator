package it.datagen.prototype.engine.constraints.builtin;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.generation.RowGenerationState;
import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.generation.domain.IntDomain;
import it.datagen.prototype.engine.generation.domain.LocalDateDomain;
import it.datagen.prototype.engine.generation.domain.LongDomain;
import it.datagen.prototype.model.rules.ParentChildComparisonRule;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parent-child comparison constraint: cross-row numeric/temporal comparison.
 *
 * <p>Enforces that a child row's column value is less than (or equal to) the parent row's
 * corresponding column value.
 *
 * <p>Generation-time strategy:
 * - During narrowing: Look up the parent row's "larger" column value from context.
 *   Restrict the child's "smaller" column domain accordingly.
 * - On row completion: Store the current row's "larger" column value keyed by child ID,
 *   for later rows to look up when they have the same parent.
 */
public final class ParentChildComparisonConstraint {

	private ParentChildComparisonConstraint() {}

	public static GenerationConstraint fromRule(ParentChildComparisonRule rule) {
		Objects.requireNonNull(rule, "rule");

		String parentIdCol = rule.getParentRelationship().getParentIdColumn();
		String childIdCol = rule.getParentRelationship().getChildIdColumn();
		String smallerCol = rule.getComparisonLogic().getSmallerColumn();
		String largerCol = rule.getComparisonLogic().getLargerColumn();
		boolean allowEqual = rule.getComparisonLogic().isAllowEqual();

		return new GenerationConstraint() {
			@Override
			public Set<String> involvedColumns() {
				return rule.requiredColumns();
			}

			@Override
			public boolean narrow(RowGenerationState state) {
				// If no parent ID, this is a root row (no constraint)
				Object parentId = state.values().get(parentIdCol);
				if (parentId == null) {
					return false;
				}

				// Look up the parent row's larger column value
				Map<Object, Object> parentValues = state.ruleProcessingContext().parentComparisonValues(rule);
				Object parentLargerValue = parentValues.get(parentId);

				// If parent row not yet generated, can't constrain (shouldn't happen in normal flow)
				if (parentLargerValue == null) {
					return false;
				}

				// Restrict the child's smaller column domain
				Domain<?> domain = state.domainFor(smallerCol);
				return restrictMaxFromValue(domain, parentLargerValue, allowEqual);
			}

			@Override
			public void onRowComplete(RowGenerationState state) {
				Object childId = state.values().get(childIdCol);
				Object largerValue = state.values().get(largerCol);

				if (childId != null && largerValue != null) {
					Map<Object, Object> parentValues = state.ruleProcessingContext().parentComparisonValues(rule);
					parentValues.put(childId, largerValue);
				}
			}
		};
	}

	/**
	 * Restricts the maximum of a domain based on a parent's value.
	 *
	 * @param domain the domain to restrict
	 * @param maxValue the maximum bound from parent
	 * @param allowEqual whether child can equal parent
	 * @return true if the domain was actually restricted
	 */
	private static boolean restrictMaxFromValue(Domain<?> domain, Object maxValue, boolean allowEqual) {
		if (domain instanceof IntDomain d && maxValue instanceof Integer v) {
			return d.restrictMaxInclusive(allowEqual ? v : v - 1);
		}
		if (domain instanceof LongDomain d && maxValue instanceof Long v) {
			long max = allowEqual ? v : v - 1L;
			return d.restrictMaxInclusive(max);
		}
		if (domain instanceof LocalDateDomain d && maxValue instanceof LocalDate v) {
			LocalDate max = allowEqual ? v : v.minusDays(1);
			return d.restrictMaxInclusive(max);
		}
		return false;
	}
}
