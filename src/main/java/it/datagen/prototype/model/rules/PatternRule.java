package it.datagen.prototype.model.rules;

import it.datagen.prototype.engine.validation.RuleValidationVisitor;
import it.datagen.prototype.model.Row;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a syntactic rule that validates a column's value against a regular
 * expression (regex) pattern.
 *
 * Example: A VAT_Number must match the pattern "[A-Z]{2}[0-9]{11}".
 */
public class PatternRule implements DataQualityRule {

	private final String columnName;
	private final Pattern pattern;

	/**
	 * Constructs a PatternRule.
	 *
	 * @param columnName   The name of the column to validate.
	 * @param regexPattern The regular expression pattern the value must match.
	 */
	public PatternRule(String columnName, String regexPattern) {
		Objects.requireNonNull(columnName, "Column name cannot be null.");
		Objects.requireNonNull(regexPattern, "Regex pattern cannot be null.");
		if (columnName.trim().isEmpty()) {
			throw new IllegalArgumentException("Column name cannot be blank.");
		}

		// Pre-compiling the regex is a best practice for performance.
		// It also validates the pattern's syntax at creation time.
		this.pattern = Pattern.compile(regexPattern);
		this.columnName = columnName;
	}

	public String getColumnName() {
		return columnName;
	}

	public Pattern getPattern() {
		return pattern;
	}

	@Override
	public boolean accept(RuleValidationVisitor visitor, Row row, List<Row> allRows) {
		return visitor.visit(this, row, allRows);
	}
}
