package it.datagen.prototype.engine.constraints;

/**
 * Internal signal used by generation-time constraints to request regenerating the current row.
 *
 * <p>This is intended for constraints that cannot be enforced via pure domain narrowing
 * (e.g., composite unique keys) without enumerating a large domain.
 */
public final class RowRegenerationRequestedException extends RuntimeException {

	public RowRegenerationRequestedException(String message) {
		super(message);
	}
}

