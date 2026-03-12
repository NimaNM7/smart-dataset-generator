package it.datagen.prototype.engine.constraints.builtin;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.generation.RowGenerationState;
import it.datagen.prototype.engine.generation.domain.StringDomain;
import it.datagen.prototype.model.rules.LengthComparisonRule;

import java.util.Objects;
import java.util.Set;

/**
 * Constraints over string length.
 */
public final class StringLengthConstraints {

	private StringLengthConstraints() {}

	public static GenerationConstraint fromRule(LengthComparisonRule rule) {
		Objects.requireNonNull(rule, "rule");
		String a = rule.getFirstColumn();
		String b = rule.getSecondColumn();

		return switch (rule.getComparison()) {
			case SHORTER_THAN_OR_EQUAL -> lengthLeq(a, b, true);
			case STRICTLY_SHORTER_THAN -> lengthLeq(a, b, false);
			case LONGER_THAN_OR_EQUAL -> lengthLeq(b, a, true); // b <= a
			case STRICTLY_LONGER_THAN -> lengthLeq(b, a, false);
		};
	}

	/**
	 * Enforce {@code len(left) <= len(right)} (or strictly when {@code allowEqual=false}).
	 */
	public static GenerationConstraint lengthLeq(String leftColumn, String rightColumn, boolean allowEqual) {
		Objects.requireNonNull(leftColumn, "leftColumn");
		Objects.requireNonNull(rightColumn, "rightColumn");

		return new GenerationConstraint() {
			@Override
			public Set<String> involvedColumns() {
				return Set.of(leftColumn, rightColumn);
			}

			@Override
			public boolean narrow(RowGenerationState state) {
				Object leftValue = state.values().get(leftColumn);
				Object rightValue = state.values().get(rightColumn);

				boolean changed = false;

				if (leftValue instanceof String ls && !(rightValue instanceof String)) {
					// right must be at least len(left) (+1 if strict)
					int min = allowEqual ? ls.length() : ls.length() + 1;
					if (state.domainFor(rightColumn) instanceof StringDomain d) {
						changed |= d.restrictMinLength(min);
					}
				} else if (!(leftValue instanceof String) && rightValue instanceof String rs) {
					// left must be at most len(right) (-1 if strict)
					int max = allowEqual ? rs.length() : rs.length() - 1;
					if (state.domainFor(leftColumn) instanceof StringDomain d) {
						changed |= d.restrictMaxLength(max);
					}
				} else if (!(leftValue instanceof String) && !(rightValue instanceof String)) {
					// Neither value present: align domains conservatively.
					if (state.domainFor(leftColumn) instanceof StringDomain l && state.domainFor(rightColumn) instanceof StringDomain r) {
						changed |= l.restrictMaxLength(r.maxLength() - (allowEqual ? 0 : 1));
						changed |= r.restrictMinLength(l.minLength() + (allowEqual ? 0 : 1));
					}
				}

				return changed;
			}
		};
	}
}

