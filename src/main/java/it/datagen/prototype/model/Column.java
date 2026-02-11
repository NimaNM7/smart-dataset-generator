package it.datagen.prototype.model;

/**
 * Represents a single column in a table schema.
 */
public class Column {
	private final String name;
	private final Class<?> type;

	/**
	 * @param name The column's name (e.g., "Employee_ID").
	 * @param type The column's Java data type (e.g., String.class).
	 */
	public Column(String name, Class<?> type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public Class<?> getType() {
		return type;
	}
}