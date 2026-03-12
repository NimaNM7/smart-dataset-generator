package it.datagen.prototype.engine.generation.domain;

public final class LongDomain implements Domain<Long> {

	private long min;
	private long max;

	public LongDomain(long min, long max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public DomainKind kind() {
		return DomainKind.LONG;
	}

	@Override
	public Class<Long> valueType() {
		return Long.class;
	}

	public long min() {
		return min;
	}

	public long max() {
		return max;
	}

	public boolean restrictMinInclusive(long newMin) {
		if (newMin <= min) {
			return false;
		}
		min = newMin;
		return true;
	}

	public boolean restrictMaxInclusive(long newMax) {
		if (newMax >= max) {
			return false;
		}
		max = newMax;
		return true;
	}

	@Override
	public boolean isEmpty() {
		return min > max;
	}
}

