package it.datagen.prototype.engine.processing;

import it.datagen.prototype.model.rules.UniqueKeyRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Shared memory for stateful rule processing (validation/fix) across rows.
 */
public class RuleProcessingContext {

	private final Map<UniqueKeyRule, Set<List<Object>>> seenUniqueKeysByRule = new IdentityHashMap<>();

	/**
	 * Registers a unique key value for a specific rule.
	 *
	 * @return true if the key was not seen before, false if it is a duplicate
	 */
	public boolean registerUniqueKey(UniqueKeyRule rule, List<Object> keyValues) {
		Set<List<Object>> seenKeysForRule = seenUniqueKeysByRule.computeIfAbsent(rule, ignored -> new HashSet<>());
		List<Object> immutableKey = List.copyOf(keyValues);
		return seenKeysForRule.add(immutableKey);
	}
}
