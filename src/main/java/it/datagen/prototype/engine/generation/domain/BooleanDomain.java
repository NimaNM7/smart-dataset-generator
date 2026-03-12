package it.datagen.prototype.engine.generation.domain;

public final class BooleanDomain implements Domain<Boolean> {

	private boolean allowTrue;
	private boolean allowFalse;

	private BooleanDomain(boolean allowTrue, boolean allowFalse) {
		this.allowTrue = allowTrue;
		this.allowFalse = allowFalse;
	}

	public static BooleanDomain any() {
		return new BooleanDomain(true, true);
	}

	@Override
	public DomainKind kind() {
		return DomainKind.BOOLEAN;
	}

	@Override
	public Class<Boolean> valueType() {
		return Boolean.class;
	}

	public boolean allowsTrue() {
		return allowTrue;
	}

	public boolean allowsFalse() {
		return allowFalse;
	}

	public boolean allowOnlyTrue() {
		if (allowTrue && !allowFalse) {
			return false;
		}
		allowTrue = true;
		allowFalse = false;
		return true;
	}

	public boolean allowOnlyFalse() {
		if (!allowTrue && allowFalse) {
			return false;
		}
		allowTrue = false;
		allowFalse = true;
		return true;
	}

	@Override
	public boolean isEmpty() {
		return !allowTrue && !allowFalse;
	}
}

