package it.datagen.prototype.engine.planner.constraints;

import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.generation.domain.StringDomain;
import it.datagen.prototype.engine.models.RuleProcessingContext;
import it.datagen.prototype.model.rules.LengthComparisonRule;

import java.util.Map;
import java.util.Set;

public final class LengthComparisonConstraint implements DomainConstraint {

	private final LengthComparisonRule rule;

	public LengthComparisonConstraint(LengthComparisonRule rule) {
		this.rule = rule;
	}

	@Override
	public Set<String> involvedColumns() {
		return rule.requiredColumns();
	}

	@Override
	public boolean narrow(Map<String, Object> values, Map<String, Domain<?>> domains, RuleProcessingContext context) {
		String first = rule.getFirstColumn();
		String second = rule.getSecondColumn();

		Object firstValue = values.get(first);
		Object secondValue = values.get(second);

		// Narrow based on known value lengths when one side is already set.
		if (firstValue instanceof CharSequence text && secondValue == null) {
			return narrowSecond(domains.get(second), text.length());
		}
		if (secondValue instanceof CharSequence text && firstValue == null) {
			return narrowFirst(domains.get(first), text.length());
		}
		return false;
	}

	private boolean narrowSecond(Domain<?> domain, int firstLength) {
		if (!(domain instanceof StringDomain d)) {
			return false;
		}
		return switch (rule.getComparison()) {
			case SHORTER_THAN_OR_EQUAL -> d.restrictMinLength(firstLength);
			case LONGER_THAN_OR_EQUAL -> d.restrictMaxLength(firstLength);
			case STRICTLY_SHORTER_THAN -> d.restrictMinLength(firstLength + 1);
			case STRICTLY_LONGER_THAN -> d.restrictMaxLength(firstLength - 1);
		};
	}

	private boolean narrowFirst(Domain<?> domain, int secondLength) {
		if (!(domain instanceof StringDomain d)) {
			return false;
		}
		return switch (rule.getComparison()) {
			case SHORTER_THAN_OR_EQUAL -> d.restrictMaxLength(secondLength);
			case LONGER_THAN_OR_EQUAL -> d.restrictMinLength(secondLength);
			case STRICTLY_SHORTER_THAN -> d.restrictMaxLength(secondLength - 1);
			case STRICTLY_LONGER_THAN -> d.restrictMinLength(secondLength + 1);
		};
	}
}

