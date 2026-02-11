package it.datagen.prototype.model.rules;

import it.datagen.prototype.engine.validation.RuleValidationVisitor;
import it.datagen.prototype.model.Row;

import java.util.List;

/**
 * Represents a unique key constraint (Rule #5). The combination of values in
 * the specified columns must be unique for each row.
 */
public class UniqueKeyRule implements DataQualityRule {

	private final List<String> columnNames;

	public UniqueKeyRule(List<String> columnNames) {
		if (columnNames == null || columnNames.isEmpty()) {
			throw new IllegalArgumentException("Column names list cannot be null or empty.");
		}
		this.columnNames = columnNames;
	}

	public List<String> getColumnNames() {
		return columnNames;
	}

	@Override
	public boolean accept(RuleValidationVisitor visitor, Row row, List<Row> allRows) {
		return visitor.visit(this, row, allRows);
	}
}
