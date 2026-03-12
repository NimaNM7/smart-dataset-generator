package it.datagen.prototype.engine.constraints.builtin;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.generation.RowGenerationState;
import it.datagen.prototype.engine.generation.domain.StringDomain;
import it.datagen.prototype.model.rules.IntraTableParentRule;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Intra-table reference constraint (parent-child within the same table).
 *
 * <p>Generation-time strategy (streaming-friendly):
 * 1. Child id is generated normally.
 * 2. Parent id, if not yet set, is sampled from already generated ids (from previous rows),
 *    or forced to null when there are no prior ids yet.
 * 3. On row completion, register the generated child id into the context.
 *
 * <p>This keeps memory O(N) in the number of generated ids, and avoids global solving.
 */
public final class IntraTableReferenceConstraint {

	private IntraTableReferenceConstraint() {}

	public static GenerationConstraint fromRule(IntraTableParentRule rule) {
		Objects.requireNonNull(rule, "rule");

		String childId = rule.getChildIdColumn();
		String parentId = rule.getParentIdColumn();

		return new GenerationConstraint() {
			@Override
			public Set<String> involvedColumns() {
				return Set.of(childId, parentId);
			}

			@Override
			public boolean narrow(RowGenerationState state) {
				// If parent already fixed (or set by some other constraint), don't override.
				if (state.values().containsKey(parentId)) {
					return false;
				}

				List<Object> previousIds = state.ruleProcessingContext().generatedIds(rule);
				if (previousIds.isEmpty()) {
					state.values().put(parentId, null);
					return true;
				}

				// If parentId is a string-like id, narrow to one of previous ids when possible.
				Object sample = sampleFromList(previousIds, state.samplingContext().random());
				// If the domain is StringDomain and sample is String, we can force it.
				if (sample == null) {
					state.values().put(parentId, null);
					return true;
				}

				if (sample instanceof String s && state.domainFor(parentId) instanceof StringDomain sd) {
					sd.allowOnly(s);
					state.values().put(parentId, s);
					return true;
				}

				// Generic forced value.
				state.values().put(parentId, sample);
				return true;
			}

			@Override
			public void onRowComplete(RowGenerationState state) {
				Object id = state.values().get(childId);
				if (id != null) {
					state.ruleProcessingContext().generatedIds(rule).add(id);
				}
			}
		};
	}

	private static Object sampleFromList(List<Object> ids, Random random) {
		if (ids.isEmpty()) {
			return null;
		}
		return ids.get(random.nextInt(ids.size()));
	}
}

