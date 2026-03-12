package it.datagen.prototype.engine.constraints.builtin;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.generation.RowGenerationState;
import it.datagen.prototype.engine.generation.domain.StringDomain;
import it.datagen.prototype.model.rules.ConditionalCompletenessRule;

import java.util.Objects;
import java.util.Set;

/**
 * Conditional nullability (or non-nullability) constraint.
 *
 * <p>Note: our current domain model doesn't represent "null allowed" explicitly.
 * For now, we implement the "must be null" direction by forcing a null value.
 * The "must be non-null" direction is left as a no-op at generation time (since
 * samplers typically produce non-null values anyway).
 */
public final class ConditionalNullabilityConstraint {

	private ConditionalNullabilityConstraint() {}

	public static GenerationConstraint fromRule(ConditionalCompletenessRule rule) {
		Objects.requireNonNull(rule, "rule");

		String conditionColumn = rule.getConditionColumn();
		Object conditionValue = rule.getConditionValue();
		String targetColumn = rule.getTargetColumn();
		boolean targetMustBeNull = rule.isTargetMustBeNull();

		return new GenerationConstraint() {
			@Override
			public Set<String> involvedColumns() {
				return Set.of(conditionColumn, targetColumn);
			}

			@Override
			public boolean narrow(RowGenerationState state) {
				Object cond = state.values().get(conditionColumn);
				if (cond == null) {
					return false;
				}
				if (!Objects.equals(cond, conditionValue)) {
					return false;
				}

				if (targetMustBeNull) {
					if (!state.values().containsKey(targetColumn) || state.values().get(targetColumn) != null) {
						state.values().put(targetColumn, null);
						return true;
					}
					return false;
				}

				// Must be non-null: best-effort narrowing.
				// We don't track nullability in domains yet, so we keep it as no-op.
				// Optionally, for strings, ensure length >= 0 (already true).
				if (state.domainFor(targetColumn) instanceof StringDomain) {
					return false;
				}
				return false;
			}
		};
	}
}

