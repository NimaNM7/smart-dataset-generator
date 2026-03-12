package it.datagen.prototype.engine.models;

import it.datagen.prototype.engine.constraints.util.TupleKey;
import it.datagen.prototype.model.rules.AggregationRule;
import it.datagen.prototype.model.rules.FunctionalDependencyRule;
import it.datagen.prototype.model.rules.IntraTableParentRule;
import it.datagen.prototype.model.rules.UniqueKeyRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared memory for rule processing.
 */
public class RuleProcessingContext {

	// Identity rules
	private final Map<FunctionalDependencyRule, Map<TupleKey, Object>> functionalDependencyMaps = new HashMap<>();
	private final Map<UniqueKeyRule, Set<TupleKey>> uniqueKeySets = new HashMap<>();

	// Structural rules
	private final Map<IntraTableParentRule, List<Object>> generatedIdsByRule = new HashMap<>();

	// Global/statistical rules
	private final Map<AggregationRule, Map<TupleKey, Double>> remainingAllowanceByGroup = new HashMap<>();

	public Map<TupleKey, Object> functionalDependencyMap(FunctionalDependencyRule rule) {
		return functionalDependencyMaps.computeIfAbsent(rule, ignored -> new HashMap<>());
	}

	public Set<TupleKey> uniqueKeySet(UniqueKeyRule rule) {
		return uniqueKeySets.computeIfAbsent(rule, ignored -> new HashSet<>());
	}

	public List<Object> generatedIds(IntraTableParentRule rule) {
		return generatedIdsByRule.computeIfAbsent(rule, ignored -> new ArrayList<>());
	}

	public Map<TupleKey, Double> remainingAllowanceByGroup(AggregationRule rule) {
		return remainingAllowanceByGroup.computeIfAbsent(rule, ignored -> new HashMap<>());
	}
}
