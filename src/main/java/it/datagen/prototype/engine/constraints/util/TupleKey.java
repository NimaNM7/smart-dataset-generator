package it.datagen.prototype.engine.constraints.util;

import java.util.Arrays;
import java.util.Objects;

/**
 * Small immutable composite key for context maps/sets.
 *
 * <p>Uses an {@code Object[]} and caches {@code hashCode} for fast reuse in hash maps/sets.
 */
public final class TupleKey {

	private final Object[] values;
	private final int hash;

	private TupleKey(Object[] values) {
		this.values = values;
		this.hash = Arrays.deepHashCode(values);
	}

	public static TupleKey of(Object[] values) {
		Objects.requireNonNull(values, "values");
		return new TupleKey(Arrays.copyOf(values, values.length));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TupleKey other)) {
			return false;
		}
		return Arrays.deepEquals(values, other.values);
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		return Arrays.deepToString(values);
	}
}

