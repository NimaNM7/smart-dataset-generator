package it.datagen.prototype.engine.planner.constraints;

import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.generation.domain.DoubleDomain;
import it.datagen.prototype.engine.generation.domain.IntDomain;
import it.datagen.prototype.engine.generation.domain.LongDomain;
import it.datagen.prototype.engine.models.RuleProcessingContext;
import it.datagen.prototype.model.rules.ColumnComparisonRule;

import java.util.Map;
import java.util.Set;

/**
 * Enforces smaller <= larger (or <) via domain narrowing.
 */
public final class ColumnComparisonConstraint implements DomainConstraint {

	private final ColumnComparisonRule rule;

	public ColumnComparisonConstraint(ColumnComparisonRule rule) {
		this.rule = rule;
	}

	@Override
	public Set<String> involvedColumns() {
		return rule.requiredColumns();
	}

	@Override
	public boolean narrow(Map<String, Object> values, Map<String, Domain<?>> domains, RuleProcessingContext context) {
		String smaller = rule.getSmallerColumn();
		String larger = rule.getLargerColumn();

		Object smallerValue = values.get(smaller);
		Object largerValue = values.get(larger);

		boolean changed = false;
		if (smallerValue instanceof Number && largerValue == null) {
			changed |= restrictMax(domains.get(larger), (Number) smallerValue, rule.isAllowEqual());
		} else if (largerValue instanceof Number && smallerValue == null) {
			changed |= restrictMin(domains.get(smaller), (Number) largerValue, rule.isAllowEqual());
		}
		return changed;
	}

	private boolean restrictMax(Domain<?> domain, Number max, boolean allowEqual) {
		if (domain instanceof IntDomain d) {
			int v = max.intValue() - (allowEqual ? 0 : 1);
			return d.restrictMaxInclusive(v);
		}
		if (domain instanceof LongDomain d) {
			long v = max.longValue() - (allowEqual ? 0 : 1);
			return d.restrictMaxInclusive(v);
		}
		if (domain instanceof DoubleDomain d) {
			double v = max.doubleValue() - (allowEqual ? 0.0d : Double.MIN_VALUE);
			return d.restrictMaxInclusive(v);
		}
		return false;
	}

	private boolean restrictMin(Domain<?> domain, Number min, boolean allowEqual) {
		if (domain instanceof IntDomain d) {
			int v = min.intValue() + (allowEqual ? 0 : 1);
			return d.restrictMinInclusive(v);
		}
		if (domain instanceof LongDomain d) {
			long v = min.longValue() + (allowEqual ? 0 : 1);
			return d.restrictMinInclusive(v);
		}
		if (domain instanceof DoubleDomain d) {
			double v = min.doubleValue() + (allowEqual ? 0.0d : Double.MIN_VALUE);
			return d.restrictMinInclusive(v);
		}
		return false;
	}
}

