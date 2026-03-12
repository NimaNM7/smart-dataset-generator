package it.datagen.prototype.engine.generation.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class EnumDomain implements Domain<Enum<?>> {

	private final Class<? extends Enum<?>> enumType;
	private List<Enum<?>> allowed;

	private EnumDomain(Class<? extends Enum<?>> enumType, List<Enum<?>> allowed) {
		this.enumType = enumType;
		this.allowed = allowed;
	}

	public static EnumDomain any(Class<? extends Enum<?>> enumType) {
		Objects.requireNonNull(enumType, "enumType");
		Enum<?>[] constants = enumType.getEnumConstants();
		if (constants == null || constants.length == 0) {
			throw new IllegalArgumentException("Enum has no constants: " + enumType.getName());
		}
		List<Enum<?>> allowed = new ArrayList<>(constants.length);
		for (Enum<?> constant : constants) {
			allowed.add(constant);
		}
		return new EnumDomain(enumType, allowed);
	}

	@Override
	public DomainKind kind() {
		return DomainKind.ENUM;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<Enum<?>> valueType() {
		return (Class<Enum<?>>) (Class<?>) Enum.class;
	}

	public Class<? extends Enum<?>> enumType() {
		return enumType;
	}

	public List<Enum<?>> allowed() {
		return allowed;
	}

	public boolean restrictAllowed(Collection<? extends Enum<?>> newAllowed) {
		if (newAllowed == null) {
			return false;
		}
		allowed = new ArrayList<>(newAllowed);
		return true;
	}

	@Override
	public boolean isEmpty() {
		return allowed.isEmpty();
	}
}
