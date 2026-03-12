package it.datagen.prototype.engine.planner.constraints;

import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.generation.domain.StringDomain;
import it.datagen.prototype.engine.models.RuleProcessingContext;
import it.datagen.prototype.model.rules.ConditionalCompletenessRule;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * If condition column equals trigger value, target must be null or non-null.
 *
 * <p>For V1 domain narrowing, we only enforce the "must be null" case and
 * provide a simple narrowing for "must be non-null" when target is a string.
 */
public final class ConditionalCompletenessConstraint implements DomainConstraint {

	private final ConditionalCompletenessRule rule;

	public ConditionalCompletenessConstraint(ConditionalCompletenessRule rule) {
		this.rule = rule;
	}

	@Override
	public Set<String> involvedColumns() {
		return rule.requiredColumns();
	}

	@Override
	public boolean narrow(Map<String, Object> values, Map<String, Domain<?>> domains, RuleProcessingContext context) {
		Object condition = values.get(rule.getConditionColumn());
		if (!Objects.equals(condition, rule.getConditionValue())) {
			return false;
		}

		String target = rule.getTargetColumn();
		if (rule.isTargetMustBeNull()) {
			if (values.containsKey(target) && values.get(target) != null) {
				// already violated; domain narrowing cannot repair this
				return false;
			}
			values.put(target, null);
			return true;
		}

		// Must be non-null: for strings, enforce minLength >= 1 as a proxy.
		Domain<?> domain = domains.get(target);
		if (domain instanceof StringDomain d) {
			return d.restrictMinLength(1);
		}
		return false;
	}
}

