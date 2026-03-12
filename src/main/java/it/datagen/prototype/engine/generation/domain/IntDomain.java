package it.datagen.prototype.engine.generation.domain;

public final class IntDomain implements Domain<Integer> {

	private int min;
	private int max;

	public IntDomain(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public DomainKind kind() {
		return DomainKind.INT;
	}

	@Override
	public Class<Integer> valueType() {
		return Integer.class;
	}

	public int min() {
		return min;
	}

	public int max() {
		return max;
	}

	public boolean restrictMinInclusive(int newMin) {
		if (newMin <= min) {
			return false;
		}
		min = newMin;
		return true;
	}

	public boolean restrictMaxInclusive(int newMax) {
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

