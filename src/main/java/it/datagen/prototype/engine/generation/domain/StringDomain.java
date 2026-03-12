package it.datagen.prototype.engine.generation.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class StringDomain implements Domain<String> {

	private int minLength;
	private int maxLength;
	private List<String> allowedValues; // optional

	public StringDomain(int minLength, int maxLength) {
		this.minLength = Math.max(0, minLength);
		this.maxLength = Math.max(this.minLength, maxLength);
	}

	@Override
	public DomainKind kind() {
		return DomainKind.STRING;
	}

	@Override
	public Class<String> valueType() {
		return String.class;
	}

	public int minLength() {
		return minLength;
	}

	public int maxLength() {
		return maxLength;
	}

	public List<String> allowedValues() {
		return allowedValues;
	}

	public boolean restrictMinLength(int newMin) {
		if (newMin <= minLength) {
			return false;
		}
		minLength = newMin;
		return true;
	}

	public boolean restrictMaxLength(int newMax) {
		if (newMax >= maxLength) {
			return false;
		}
		maxLength = newMax;
		return true;
	}

	public boolean allowOnly(String value) {
		if (value == null) {
			return false;
		}
		allowedValues = List.of(value);
		minLength = value.length();
		maxLength = value.length();
		return true;
	}

	public boolean restrictAllowedValues(Collection<String> values) {
		if (values == null) {
			return false;
		}
		allowedValues = new ArrayList<>(values);
		return true;
	}

	@Override
	public boolean isEmpty() {
		if (minLength > maxLength) {
			return true;
		}
		return allowedValues != null && allowedValues.isEmpty();
	}
}

