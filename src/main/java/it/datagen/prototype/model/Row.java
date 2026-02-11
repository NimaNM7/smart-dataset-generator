package it.datagen.prototype.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single row of data in a table. It's essentially a map from
 * column names (String) to their values (Object).
 */
public class Row {

	private final Map<String, Object> values;

	public Row() {
		this.values = new HashMap<>();
	}

	/**
	 * Sets a value for a specific column in this row.
	 * 
	 * @param columnName The name of the column.
	 * @param value      The value to set.
	 */
	public void setValue(String columnName, Object value) {
		values.put(columnName, value);
	}

	/**
	 * Retrieves the value of a specific column from this row.
	 * 
	 * @param columnName The name of the column.
	 * @return The value associated with the column, or null if not set.
	 */
	public Object getValue(String columnName) {
		return values.get(columnName);
	}

	@Override
	public String toString() {
		return values.toString();
	}
}