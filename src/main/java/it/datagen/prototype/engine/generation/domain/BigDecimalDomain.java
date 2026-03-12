package it.datagen.prototype.engine.generation.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class BigDecimalDomain implements Domain<BigDecimal> {

	private BigDecimal min;
	private BigDecimal max;
	private final int scale;

	public BigDecimalDomain(BigDecimal min, BigDecimal max, int scale) {
		this.min = Objects.requireNonNull(min, "min");
		this.max = Objects.requireNonNull(max, "max");
		this.scale = scale;
	}

	@Override
	public DomainKind kind() {
		return DomainKind.BIG_DECIMAL;
	}

	@Override
	public Class<BigDecimal> valueType() {
		return BigDecimal.class;
	}

	public BigDecimal min() {
		return min;
	}

	public BigDecimal max() {
		return max;
	}

	public int scale() {
		return scale;
	}

	public boolean restrictMinInclusive(BigDecimal newMin) {
		if (newMin == null || newMin.compareTo(min) <= 0) {
			return false;
		}
		min = newMin;
		return true;
	}

	public boolean restrictMaxInclusive(BigDecimal newMax) {
		if (newMax == null || newMax.compareTo(max) >= 0) {
			return false;
		}
		max = newMax;
		return true;
	}

	@Override
	public boolean isEmpty() {
		return min.compareTo(max) > 0;
	}
}

