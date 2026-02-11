package it.datagen.prototype.model.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Functional Dependency (FD) rule (e.g., A -> B). This rule states
 * that for any two rows in the table, if their values for the determinant
 * columns are the same, then their values for the dependent column must also be
 * the same.
 * 
 * Example: In a table of addresses, (Postcode) -> (City).
 */
public class FunctionalDependencyRule implements DataQualityRule {

	private final List<String> determinantColumns; // The "A" part of the dependency
	private final String dependentColumn; // The "B" part of the dependency

	/**
	 * Constructs a FunctionalDependencyRule.
	 *
	 * @param determinantColumns A list of column names that uniquely determine the
	 *                           dependent column. Cannot be null or empty.
	 * @param dependentColumn    The column whose value is determined by the
	 *                           determinant columns. Cannot be null or blank.
	 */
	public FunctionalDependencyRule(List<String> determinantColumns, String dependentColumn) {
		Objects.requireNonNull(determinantColumns, "Determinant columns list cannot be null.");
		Objects.requireNonNull(dependentColumn, "Dependent column name cannot be null.");

		if (determinantColumns.isEmpty()) {
			throw new IllegalArgumentException("Determinant columns list cannot be empty.");
		}
		if (dependentColumn.trim().isEmpty()) {
			throw new IllegalArgumentException("Dependent column name cannot be blank.");
		}
		if (determinantColumns.contains(dependentColumn)) {
			throw new IllegalArgumentException("The dependent column cannot be one of the determinant columns.");
		}

		this.determinantColumns = Collections.unmodifiableList(new ArrayList<>(determinantColumns));
		this.dependentColumn = dependentColumn;
	}

	public List<String> getDeterminantColumns() {
		return determinantColumns;
	}

	public String getDependentColumn() {
		return dependentColumn;
	}
}