package it.datagen.prototype.engine.constraints.builtin;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.generation.RowGenerationState;
import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.generation.domain.IntDomain;
import it.datagen.prototype.engine.generation.domain.LocalDateDomain;
import it.datagen.prototype.engine.generation.domain.LongDomain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Generic relational constraints that can be used for many different rules.
 *
 * <p>These constraints are rule-agnostic: they only know columns and a relation.
 */
public final class RelationalConstraints {

	private RelationalConstraints() {}

	/**
	 * Enforce {@code left <= right} (or {@code left < right} when {@code allowEqual=false}).
	 *
	 * <p>Narrows the domain of the not-yet-generated side when the other side has a value.
	 * If neither side has a value, it will narrow both domains to keep feasibility.
	 */
	public static GenerationConstraint leq(String leftColumn, String rightColumn, boolean allowEqual) {
		Objects.requireNonNull(leftColumn, "leftColumn");
		Objects.requireNonNull(rightColumn, "rightColumn");
		if (leftColumn.isBlank() || rightColumn.isBlank()) {
			throw new IllegalArgumentException("Column names must be non-blank.");
		}
		if (leftColumn.equals(rightColumn)) {
			throw new IllegalArgumentException("Columns must be different.");
		}

		return new GenerationConstraint() {
			@Override
			public Set<String> involvedColumns() {
				return Set.of(leftColumn, rightColumn);
			}

			@Override
			public boolean narrow(RowGenerationState state) {
				Object leftValue = state.values().get(leftColumn);
				Object rightValue = state.values().get(rightColumn);

				Domain<?> leftDomain = state.domainFor(leftColumn);
				Domain<?> rightDomain = state.domainFor(rightColumn);

				boolean changed = false;

				if (leftValue != null && rightValue == null) {
					changed |= restrictMinFromValue(rightDomain, leftValue, allowEqual);
				} else if (leftValue == null && rightValue != null) {
					changed |= restrictMaxFromValue(leftDomain, rightValue, allowEqual);
				} else if (leftValue == null && rightValue == null) {
					// Narrow both domains to keep them consistent without forcing a value.
					changed |= restrictDomainsPair(leftDomain, rightDomain, allowEqual);
				}

				return changed;
			}
		};
	}

	private static boolean restrictMinFromValue(Domain<?> domain, Object minValue, boolean allowEqual) {
		if (domain instanceof IntDomain d && minValue instanceof Integer v) {
			return d.restrictMinInclusive(allowEqual ? v : v + 1);
		}
		if (domain instanceof LongDomain d && minValue instanceof Long v) {
			long min = allowEqual ? v : v + 1L;
			return d.restrictMinInclusive(min);
		}
		if (domain instanceof LocalDateDomain d && minValue instanceof LocalDate v) {
			LocalDate min = allowEqual ? v : v.plusDays(1);
			return d.restrictMinInclusive(min);
		}
		return false;
	}

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

	private static boolean restrictDomainsPair(Domain<?> left, Domain<?> right, boolean allowEqual) {
		boolean changed = false;

		if (left instanceof IntDomain l && right instanceof IntDomain r) {
			// Ensure feasibility without forcing either side.
			changed |= l.restrictMaxInclusive(r.max());
			changed |= r.restrictMinInclusive(allowEqual ? l.min() : l.min() + 1);
			return changed;
		}
		if (left instanceof LongDomain l && right instanceof LongDomain r) {
			changed |= l.restrictMaxInclusive(r.max());
			changed |= r.restrictMinInclusive(allowEqual ? l.min() : l.min() + 1L);
			return changed;
		}
		if (left instanceof LocalDateDomain l && right instanceof LocalDateDomain r) {
			changed |= l.restrictMaxInclusive(r.max());
			changed |= r.restrictMinInclusive(allowEqual ? l.min() : l.min().plusDays(1));
			return changed;
		}

		return false;
	}
}
