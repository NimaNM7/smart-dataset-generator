package it.datagen.prototype.engine.generation.domain;

public final class BytesDomain implements Domain<byte[]> {

	private int minLength;
	private int maxLength;

	public BytesDomain(int minLength, int maxLength) {
		this.minLength = Math.max(0, minLength);
		this.maxLength = Math.max(this.minLength, maxLength);
	}

	@Override
	public DomainKind kind() {
		return DomainKind.BYTES;
	}

	@Override
	public Class<byte[]> valueType() {
		return byte[].class;
	}

	public int minLength() {
		return minLength;
	}

	public int maxLength() {
		return maxLength;
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

	@Override
	public boolean isEmpty() {
		return minLength > maxLength;
	}
}

