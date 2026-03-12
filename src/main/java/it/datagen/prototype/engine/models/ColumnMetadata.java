package it.datagen.prototype.engine.models;

import java.util.Objects;

public class ColumnMetadata {

	private final String name;
	private final Class<?> type;

	public ColumnMetadata(String name, Class<?> type) {
		this.name = Objects.requireNonNull(name, "name");
		this.type = Objects.requireNonNull(type, "type");
	}

	public String getName() {
		return name;
	}

	public Class<?> getType() {
		return type;
	}
}
