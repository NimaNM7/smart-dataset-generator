package it.datagen.prototype.engine.constraints.builtin;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.constraints.RowRegenerationRequestedException;
import it.datagen.prototype.engine.generation.RowGenerationState;
import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.generation.domain.StringDomain;
import it.datagen.prototype.model.rules.SiblingEqualityRule;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Sibling equality constraint: rows sharing a parent must have equal target column values.
 *
 * <p>Generation-time strategy:
 * - During narrowing: Once the parent ID is known, look up the stored value for that parent.
 *   If it exists, force the target column to match and narrow its domain to singleton.
 * - On row completion: Store or validate the mapping of parent ID -> target column value.
 */
public final class SiblingEqualityConstraint {

	private SiblingEqualityConstraint() {}

	public static GenerationConstraint fromRule(SiblingEqualityRule rule) {
		Objects.requireNonNull(rule, "rule");

		String parentIdCol = rule.getParentRelationship().getParentIdColumn();
		String targetCol = rule.getTargetColumn();

		return new GenerationConstraint() {
			@Override
			public Set<String> involvedColumns() {
				return rule.requiredColumns();
			}

			@Override
			public boolean narrow(RowGenerationState state) {
				// Only act once the parent ID is known
				Object parentId = state.values().get(parentIdCol);
				if (parentId == null) {
					return false;
				}

				// Look up the stored value for this parent
				Map<Object, Object> siblingMap = state.ruleProcessingContext().siblingValueMap(rule);
				Object storedValue = siblingMap.get(parentId);

				// If no stored value yet, nothing to enforce
				if (storedValue == null) {
					return false;
				}

				// If target column already has the correct value, no change
				Object currentValue = state.values().get(targetCol);
				if (Objects.equals(currentValue, storedValue)) {
					return false;
				}

				// Force the target column to match
				state.values().put(targetCol, storedValue);

				// Narrow the domain to singleton if it's a StringDomain
				Domain<?> domain = state.domainFor(targetCol);
				if (domain instanceof StringDomain sd && storedValue instanceof String s) {
					sd.allowOnly(s);
				}

				return true;
			}

			@Override
			public void onRowComplete(RowGenerationState state) {
				Object parentId = state.values().get(parentIdCol);
				if (parentId == null) {
					return;
				}

				Object targetValue = state.values().get(targetCol);
				Map<Object, Object> siblingMap = state.ruleProcessingContext().siblingValueMap(rule);

				Object existing = siblingMap.putIfAbsent(parentId, targetValue);
				if (existing != null && !Objects.equals(existing, targetValue)) {
					throw new RowRegenerationRequestedException(
							"Sibling equality violation: parent=" + parentId
									+ " expected=" + existing
									+ " got=" + targetValue
					);
				}
			}
		};
	}
}
