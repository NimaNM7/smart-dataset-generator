package it.datagen.prototype.engine.generation;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.constraints.RowRegenerationRequestedException;
import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.models.ExecutionPlan;
import it.datagen.prototype.engine.models.RuleProcessingContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain-first row generation engine based on an {@link ExecutionPlan}.
 */
public final class RowGenerationEngine {

	private final ExecutionPlan plan;
	private final Map<String, Class<?>> columnTypes;
	private final int maxPropagationPasses;
	private final int maxSccRetries;

	public RowGenerationEngine(ExecutionPlan plan, int maxPropagationPasses, int maxSccRetries) {
		this.plan = Objects.requireNonNull(plan, "plan");
		this.columnTypes = plan.getColumns().stream()
				.collect(Collectors.toMap(
						c -> c.getName(),
						c -> c.getType()
				));
		this.maxPropagationPasses = Math.max(1, maxPropagationPasses);
		this.maxSccRetries = Math.max(1, maxSccRetries);
	}

	public Map<String, Object> generateRow(SamplingContext samplingContext, RuleProcessingContext context) {
		for (int attempt = 0; attempt < Math.max(1, maxSccRetries); attempt++) {
			RowGenerationState state = new RowGenerationState(columnTypes, samplingContext, context);

			for (Set<String> component : plan.getColumnComponentsInOrder()) {
				if (component.size() == 1) {
					String column = component.iterator().next();
					generateSingleton(column, state);
				} else {
					generateScc(component, state);
				}
			}

			// Post-row hooks (context-backed constraints).
			try {
				for (GenerationConstraint c : plan.getConstraints()) {
					c.onRowComplete(state);
				}
			} catch (RowRegenerationRequestedException e) {
				// Retry generating a new row from scratch.

				// log to see which rule is making us hard to generate dataset
				if (attempt == maxSccRetries - 1)
					System.out.println("hard rule: " + e.getMessage());
				continue;
			}

			return state.values();
		}

		throw new IllegalStateException("Unable to generate a valid row after retries.");
	}

	private void generateSingleton(String column, RowGenerationState state) {
		state.domainFor(column);
		propagateForColumns(Set.of(column), state);
		if (!state.values().containsKey(column)) {
			Object value = SamplerRegistry.sample(state.domainFor(column), state.samplingContext());
			state.values().put(column, value);
		}
	}

	private void generateScc(Set<String> component, RowGenerationState state) {
		List<String> columns = component.stream().sorted().toList();
		for (int attempt = 0; attempt < maxSccRetries; attempt++) {
			// Reset only SCC columns and regenerate them together.
			for (String col : columns) {
				state.values().remove(col);
				state.resetDomain(col);
			}

			if (!propagateForColumns(component, state)) {
				continue;
			}

			boolean ok = true;
			for (String col : columns) {
				if (!propagateForColumns(component, state)) {
					ok = false;
					break;
				}
				Domain<?> domain = state.domainFor(col);
				if (domain.isEmpty()) {
					ok = false;
					break;
				}
				Object value = SamplerRegistry.sample(domain, state.samplingContext());
				state.values().put(col, value);
			}

			if (!ok) {
				continue;
			}
			return;
		}

		throw new IllegalStateException("Unable to generate SCC component after retries: " + component);
	}

	private boolean propagateForColumns(Set<String> columns, RowGenerationState state) {
		List<GenerationConstraint> constraints = componentConstraints(columns);
		for (int pass = 0; pass < maxPropagationPasses; pass++) {
			boolean changed = false;
			for (GenerationConstraint constraint : constraints) {
				changed |= constraint.narrow(state);
			}
			boolean anyEmpty = columns.stream().anyMatch(col -> state.domainFor(col).isEmpty());
			if (anyEmpty) {
				return false;
			}
			if (!changed) {
				return true;
			}
		}
		return true;
	}

	private List<GenerationConstraint> componentConstraints(Set<String> columns) {
		Set<GenerationConstraint> unique = new LinkedHashSet<>();
		for (String col : columns) {
			List<GenerationConstraint> list = plan.getConstraintIndex().get(col);
			if (list != null) {
				unique.addAll(list);
			}
		}
		return new ArrayList<>(unique);
	}
}
