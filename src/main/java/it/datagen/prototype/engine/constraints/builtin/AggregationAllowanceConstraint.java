package it.datagen.prototype.engine.constraints.builtin;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.constraints.util.TupleKey;
import it.datagen.prototype.engine.generation.RowGenerationState;
import it.datagen.prototype.engine.generation.domain.DoubleDomain;
import it.datagen.prototype.engine.generation.domain.IntDomain;
import it.datagen.prototype.engine.generation.domain.LongDomain;
import it.datagen.prototype.model.rules.AggregationRule;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Streaming-friendly aggregation constraint.
 *
 * <p>Current supported form:
 * group-by columns + SUM(aggregationColumn) <= threshold
 *
 * <p>We maintain remaining allowance per group in the context and narrow the
 * aggregation column's domain accordingly for the current row.
 */
public final class AggregationAllowanceConstraint {

	private AggregationAllowanceConstraint() {}

	public static GenerationConstraint fromRule(AggregationRule rule) {
		Objects.requireNonNull(rule, "rule");

		if (rule.getFunction() != AggregationRule.AggregationFunction.SUM
				|| rule.getOperator() != AggregationRule.ComparisonOperator.LESS_THAN_OR_EQUAL) {
			// For V1, we only implement one representative global constraint.
			return new GenerationConstraint() {
				@Override
				public Set<String> involvedColumns() {
					return rule.requiredColumns();
				}

				@Override
				public boolean narrow(RowGenerationState state) {
					return false;
				}
			};
		}

		List<String> groupBy = rule.getGroupByColumns();
		String aggCol = rule.getAggregationColumn();
		double threshold = rule.getThreshold();

		return new GenerationConstraint() {
			@Override
			public Set<String> involvedColumns() {
				return rule.requiredColumns();
			}

			@Override
			public boolean narrow(RowGenerationState state) {
				// Only narrow once group key is known (all groupBy values present).
				Object[] parts = new Object[groupBy.size()];
				for (int i = 0; i < groupBy.size(); i++) {
					Object v = state.values().get(groupBy.get(i));
					if (v == null) {
						return false;
					}
					parts[i] = v;
				}
				TupleKey groupKey = TupleKey.of(parts);

				Map<TupleKey, Double> remaining = state.ruleProcessingContext().remainingAllowanceByGroup(rule);
				double remainingAllowance = remaining.computeIfAbsent(groupKey, ignored -> threshold);

				boolean changed = false;
				if (state.domainFor(aggCol) instanceof IntDomain d) {
					changed |= d.restrictMaxInclusive((int) Math.floor(remainingAllowance));
				} else if (state.domainFor(aggCol) instanceof LongDomain d) {
					changed |= d.restrictMaxInclusive((long) Math.floor(remainingAllowance));
				} else if (state.domainFor(aggCol) instanceof DoubleDomain d) {
					changed |= d.restrictMaxInclusive(remainingAllowance);
				}
				return changed;
			}

			@Override
			public void onRowComplete(RowGenerationState state) {
				Object[] parts = new Object[groupBy.size()];
				for (int i = 0; i < groupBy.size(); i++) {
					parts[i] = state.values().get(groupBy.get(i));
				}
				TupleKey groupKey = TupleKey.of(parts);

				Object value = state.values().get(aggCol);
				if (!(value instanceof Number n)) {
					return;
				}

				Map<TupleKey, Double> remaining = state.ruleProcessingContext().remainingAllowanceByGroup(rule);
				double current = remaining.computeIfAbsent(groupKey, ignored -> threshold);
				double next = current - n.doubleValue();
				remaining.put(groupKey, next);
			}
		};
	}
}

