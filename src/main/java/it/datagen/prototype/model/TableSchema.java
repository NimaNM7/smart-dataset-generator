package it.datagen.prototype.model;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Represents the schema of a database table.
 */
public class TableSchema {

	private final String tableName;
	private final List<Column> columns;
	private final transient Map<String, Column> columnMap; // For quick access by name

	/**
	 * @param tableName The table's name (e.g., "Employees").
	 * @param columns   A list of Column objects defining the table structure.
	 */
	public TableSchema(String tableName, List<Column> columns) {
		this.tableName = tableName;
		this.columns = columns;
		this.columnMap = columns.stream().collect(Collectors.toMap(Column::getName, col -> col));
	}

	public String getTableName() {
		return tableName;
	}

	public List<Column> getColumns() {
		return columns;
	}

	/**
	 * Retrieves a Column object by its name.
	 * 
	 * @param name The name of the column to find.
	 * @return The corresponding Column object, or null if not found.
	 */
	public Column getColumn(String name) {
		return columnMap.get(name);
	}

	/**
	 * Exposes the schema's column map (name -> Column) as a read-only view.
	 *
	 * @return unmodifiable column map
	 */
	public Map<String, Column> getColumnMap() {
		return Collections.unmodifiableMap(columnMap);
	}
}
