package it.datagen.prototype.engine.planner.constraints;

import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.models.RuleProcessingContext;

import java.util.Map;
import java.util.Set;

/**
 * Narrows domains (and sometimes assigns values) based on current row state.
 *
 * <p>This is the core "domain-first" mechanism: constraints try to prevent violations
 * by shrinking feasible sets before sampling.
 */
public interface DomainConstraint {

	Set<String> involvedColumns();

	/**
	 * Applies narrowing for this constraint.
	 *
	 * @param values current row values (mutable)
	 * @param domains current row domains (mutable)
	 * @param context shared runtime memory for identity/global rules
	 * @return true if any domain/value changed
	 */
	boolean narrow(Map<String, Object> values, Map<String, Domain<?>> domains, RuleProcessingContext context);
}

