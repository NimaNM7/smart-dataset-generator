package it.datagen.prototype.engine.generation.domain;

import java.util.UUID;

public final class UuidDomain implements Domain<UUID> {

	@Override
	public DomainKind kind() {
		return DomainKind.UUID;
	}

	@Override
	public Class<UUID> valueType() {
		return UUID.class;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}
}

