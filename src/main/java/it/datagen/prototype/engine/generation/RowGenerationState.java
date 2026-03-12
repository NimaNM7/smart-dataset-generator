package it.datagen.prototype.engine.generation;

import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.generation.domain.Domains;
import it.datagen.prototype.engine.models.RuleProcessingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable per-row state used by the domain-first generation engine.
 */
public final class RowGenerationState {

	private final Map<String, Object> values;
	private final Map<String, Domain<?>> domains;
	private final Map<String, Class<?>> columnTypes;
	private final SamplingContext samplingContext;
	private final RuleProcessingContext ruleProcessingContext;

	public RowGenerationState(Map<String, Class<?>> columnTypes, SamplingContext samplingContext,
			RuleProcessingContext ruleProcessingContext) {
		this.values = new HashMap<>();
		this.domains = new HashMap<>();
		this.columnTypes = Objects.requireNonNull(columnTypes, "columnTypes");
		this.samplingContext = Objects.requireNonNull(samplingContext, "samplingContext");
		this.ruleProcessingContext = Objects.requireNonNull(ruleProcessingContext, "ruleProcessingContext");
	}

	public Map<String, Object> values() {
		return values;
	}

	public Map<String, Domain<?>> domains() {
		return domains;
	}

	public SamplingContext samplingContext() {
		return samplingContext;
	}

	public RuleProcessingContext ruleProcessingContext() {
		return ruleProcessingContext;
	}

	public Domain<?> domainFor(String columnName) {
		return domains.computeIfAbsent(columnName, name -> Domains.defaultDomainFor(columnTypes.get(name)));
	}

	public void resetDomain(String columnName) {
		domains.put(columnName, Domains.defaultDomainFor(columnTypes.get(columnName)));
	}
}

