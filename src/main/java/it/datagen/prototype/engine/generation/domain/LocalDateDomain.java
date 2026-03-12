package it.datagen.prototype.engine.generation.domain;

import java.time.LocalDate;
import java.util.Objects;

public final class LocalDateDomain implements Domain<LocalDate> {

	private LocalDate min;
	private LocalDate max;

	public LocalDateDomain(LocalDate min, LocalDate max) {
		this.min = Objects.requireNonNull(min, "min");
		this.max = Objects.requireNonNull(max, "max");
	}

	@Override
	public DomainKind kind() {
		return DomainKind.LOCAL_DATE;
	}

	@Override
	public Class<LocalDate> valueType() {
		return LocalDate.class;
	}

	public LocalDate min() {
		return min;
	}

	public LocalDate max() {
		return max;
	}

	public boolean restrictMinInclusive(LocalDate newMin) {
		if (newMin == null || !newMin.isAfter(min)) {
			return false;
		}
		min = newMin;
		return true;
	}

	public boolean restrictMaxInclusive(LocalDate newMax) {
		if (newMax == null || !newMax.isBefore(max)) {
			return false;
		}
		max = newMax;
		return true;
	}

	@Override
	public boolean isEmpty() {
		return min.isAfter(max);
	}
}

