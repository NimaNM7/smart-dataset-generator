package it.datagen.prototype.model.rules;

import java.util.Collections;
import java.util.Set;

/**
 * Base interface for all data quality rules.
 */
public interface DataQualityRule {

	/**
	 * Returns the columns required to evaluate this rule.
	 *
	 * <p>Rules without direct column references can keep the default empty set.
	 *
	 * @return required column names
	 */
	default Set<String> requiredColumns() {
		return Collections.emptySet();
	}
}
