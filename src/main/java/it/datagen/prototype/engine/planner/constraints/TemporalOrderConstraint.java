package it.datagen.prototype.engine.planner.constraints;

import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.generation.domain.LocalDateDomain;
import it.datagen.prototype.engine.models.RuleProcessingContext;
import it.datagen.prototype.model.rules.TemporalOrderRule;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public final class TemporalOrderConstraint implements DomainConstraint {

	private final TemporalOrderRule rule;

	public TemporalOrderConstraint(TemporalOrderRule rule) {
		this.rule = rule;
	}

	@Override
	public Set<String> involvedColumns() {
		return rule.requiredColumns();
	}

	@Override
	public boolean narrow(Map<String, Object> values, Map<String, Domain<?>> domains, RuleProcessingContext context) {
		String earlier = rule.getEarlierDateColumn();
		String later = rule.getLaterDateColumn();

		Object earlierValue = values.get(earlier);
		Object laterValue = values.get(later);

		boolean changed = false;
		if (earlierValue instanceof LocalDate date && laterValue == null) {
			LocalDate max = rule.isAllowEqual() ? date : date.minusDays(1);
			changed |= restrictMinDate(domains.get(later), date);
			changed |= restrictMaxDate(domains.get(earlier), max); // no-op in typical flow
		} else if (laterValue instanceof LocalDate date && earlierValue == null) {
			LocalDate max = rule.isAllowEqual() ? date : date.minusDays(1);
			changed |= restrictMaxDate(domains.get(earlier), max);
		}
		return changed;
	}

	private boolean restrictMinDate(Domain<?> domain, LocalDate min) {
		if (domain instanceof LocalDateDomain d) {
			return d.restrictMinInclusive(min);
		}
		return false;
	}

	private boolean restrictMaxDate(Domain<?> domain, LocalDate max) {
		if (domain instanceof LocalDateDomain d) {
			return d.restrictMaxInclusive(max);
		}
		return false;
	}
}

