package it.datagen.prototype.model.rules;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

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
	public Set<String> requiredColumns() {
		return Set.copyOf(new LinkedHashSet<>(columnNames));
	}

	@Override
	public RuleCategory category() {
		return RuleCategory.IDENTITY;
	}
}
