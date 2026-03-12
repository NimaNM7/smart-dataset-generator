package it.datagen.prototype.engine.constraints.builtin;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.constraints.RowRegenerationRequestedException;
import it.datagen.prototype.engine.constraints.util.TupleKey;
import it.datagen.prototype.engine.generation.RowGenerationState;
import it.datagen.prototype.model.rules.UniqueKeyRule;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Composite unique key constraint.
 *
 * <p>Uniqueness is enforced using the per-table context. At generation time this
 * cannot be expressed as a small domain restriction in general, so we enforce
 * it on row completion and request row regeneration when a duplicate occurs.
 */
public final class UniqueKeyConstraint {

	private UniqueKeyConstraint() {}

	public static GenerationConstraint fromRule(UniqueKeyRule rule) {
		Objects.requireNonNull(rule, "rule");
		List<String> cols = rule.getColumnNames();

		return new GenerationConstraint() {
			@Override
			public Set<String> involvedColumns() {
				return rule.requiredColumns();
			}

			@Override
			public boolean narrow(RowGenerationState state) {
				// No meaningful narrowing without enumerating large discrete spaces.
				return false;
			}

			@Override
			public void onRowComplete(RowGenerationState state) {
				Object[] parts = new Object[cols.size()];
				for (int i = 0; i < cols.size(); i++) {
					parts[i] = state.values().get(cols.get(i));
				}
				TupleKey key = TupleKey.of(parts);

				Set<TupleKey> seen = state.ruleProcessingContext().uniqueKeySet(rule);
				if (!seen.add(key)) {
					throw new RowRegenerationRequestedException("Duplicate unique key for rule columns=" + cols + " key=" + key);
				}
			}
		};
	}
}

