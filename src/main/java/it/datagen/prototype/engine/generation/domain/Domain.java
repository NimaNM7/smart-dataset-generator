package it.datagen.prototype.engine.generation.domain;

/**
 * Represents the current feasible set of values for one column during generation.
 *
 * <p>Domains are mutable on purpose: narrowing should be cheap and avoid allocations.
 */
public interface Domain<T> {

	DomainKind kind();

	Class<T> valueType();

	boolean isEmpty();
}
