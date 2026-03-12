package it.datagen.prototype.engine.constraints.builtin;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.constraints.util.TupleKey;
import it.datagen.prototype.engine.generation.RowGenerationState;
import it.datagen.prototype.model.rules.FunctionalDependencyRule;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Functional dependency constraint: X -> Y.
 *
 * <p>Uses {@link it.datagen.prototype.engine.models.RuleProcessingContext} to store
 * the mapping from determinant tuple to dependent value.
 *
 * <p>Generation-time strategy:
 * if determinant values are present and we've seen them before, force Y to the mapped value.
 * Otherwise, do nothing during narrowing; on row completion, record the mapping.
 */
public final class FunctionalDependencyConstraint {

	private FunctionalDependencyConstraint() {}

	public static GenerationConstraint fromRule(FunctionalDependencyRule rule) {
		Objects.requireNonNull(rule, "rule");

		List<String> det = rule.getDeterminantColumns();
		String dep = rule.getDependentColumn();

		return new GenerationConstraint() {
			@Override
			public Set<String> involvedColumns() {
				// determinants + dependent
				return rule.requiredColumns();
			}

			@Override
			public boolean narrow(RowGenerationState state) {
				// Only enforce when determinants are all known.
				Object[] keyParts = new Object[det.size()];
				for (int i = 0; i < det.size(); i++) {
					Object v = state.values().get(det.get(i));
					if (v == null) {
						return false;
					}
					keyParts[i] = v;
				}

				Map<TupleKey, Object> map = state.ruleProcessingContext().functionalDependencyMap(rule);
				Object mapped = map.get(TupleKey.of(keyParts));
				if (mapped == null) {
					return false;
				}

				if (!state.values().containsKey(dep) || !Objects.equals(state.values().get(dep), mapped)) {
					state.values().put(dep, mapped);
					return true;
				}
				return false;
			}

			@Override
			public void onRowComplete(RowGenerationState state) {
				Object[] keyParts = new Object[det.size()];
				for (int i = 0; i < det.size(); i++) {
					keyParts[i] = state.values().get(det.get(i));
				}
				Object dependentValue = state.values().get(dep);
				if (dependentValue == null) {
					return;
				}

				Map<TupleKey, Object> map = state.ruleProcessingContext().functionalDependencyMap(rule);
				map.putIfAbsent(TupleKey.of(keyParts), dependentValue);
			}
		};
	}
}

