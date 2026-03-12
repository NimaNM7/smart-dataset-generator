package it.datagen.prototype.engine.generation.domain;

public final class DoubleDomain implements Domain<Double> {

	private double min;
	private double max;

	public DoubleDomain(double min, double max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public DomainKind kind() {
		return DomainKind.DOUBLE;
	}

	@Override
	public Class<Double> valueType() {
		return Double.class;
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public boolean restrictMinInclusive(double newMin) {
		if (newMin <= min) {
			return false;
		}
		min = newMin;
		return true;
	}

	public boolean restrictMaxInclusive(double newMax) {
		if (newMax >= max) {
			return false;
		}
		max = newMax;
		return true;
	}

	@Override
	public boolean isEmpty() {
		return Double.isNaN(min) || Double.isNaN(max) || min > max;
	}
}

