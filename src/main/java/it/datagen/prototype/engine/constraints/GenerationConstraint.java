package it.datagen.prototype.engine.constraints;

import it.datagen.prototype.engine.generation.RowGenerationState;

import java.util.Collections;
import java.util.Set;

/**
 * A generation-time constraint.
 *
 * <p>Constraints are runtime objects used by the generator to narrow domains and/or
 * interact with the {@code RuleProcessingContext}. They are intentionally not tied
 * to rule types once compiled.
 *
 * <p>This layer is generation-focused: it does not perform "validation" of final
 * datasets. The goal is to avoid violations by narrowing domains and sampling
 * inside them.
 */
public interface GenerationConstraint {

	/**
	 * Columns touched by this constraint (for indexing).
	 */
	Set<String> involvedColumns();

	/**
	 * Attempts to narrow domains and/or set forced values based on already available values.
	 *
	 * @return true if the state changed (domain narrowed or a value was set)
	 */
	boolean narrow(RowGenerationState state);

	/**
	 * Hook invoked after a row is fully generated, for constraints that need per-table memory
	 * (e.g., unique keys, functional dependencies, aggregation budgets, intra-table references).
	 */
	default void onRowComplete(RowGenerationState state) {
		// no-op
	}

	/**
	 * Convenience for constraints that only apply to one column.
	 */
	static Set<String> oneColumn(String column) {
		return Collections.singleton(column);
	}
}

