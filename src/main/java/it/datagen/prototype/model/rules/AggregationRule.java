package it.datagen.prototype.model.rules;

import it.datagen.prototype.engine.validation.RuleValidationVisitor;
import it.datagen.prototype.model.Row;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a rule that checks a constraint on an aggregated value over a
 * group of rows (Rule #12). This rule is applied after grouping the rows by a
 * set of key columns.
 *
 * Example: Group by 'Parent_Project_ID', the SUM of all 'Quota' values for the
 * children must be less than or equal to 100.
 */
public class AggregationRule implements DataQualityRule {

	public enum AggregationFunction {
		SUM, AVERAGE, COUNT
	}

	public enum ComparisonOperator {
		LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL, EQUAL, STRICTLY_LESS_THAN, STRICTLY_GREATER_THAN
	}

	private final List<String> groupByColumns; // The columns to group by (e.g., "Parent_Project_ID").
	private final String aggregationColumn; // The column to apply the function to (e.g., "Quota").
	private final AggregationFunction function; // The aggregation function to apply (e.g., SUM).
	private final ComparisonOperator operator; // The comparison to perform (e.g., LESS_THAN_OR_EQUAL).
	private final double threshold; // The value to compare against (e.g., 100).

	/**
	 * Constructs an AggregationRule.
	 *
	 * @param groupByColumns    The list of column names used for grouping.
	 * @param aggregationColumn The name of the column on which the aggregation is
	 *                          performed.
	 * @param function          The aggregation function (SUM, AVERAGE, etc.).
	 * @param operator          The comparison operator.
	 * @param threshold         The threshold value for the comparison.
	 */
	public AggregationRule(List<String> groupByColumns, String aggregationColumn, AggregationFunction function,
			ComparisonOperator operator, double threshold) {

		Objects.requireNonNull(groupByColumns, "Group-by columns list cannot be null.");
		Objects.requireNonNull(aggregationColumn, "Aggregation column name cannot be null.");
		Objects.requireNonNull(function, "Aggregation function cannot be null.");
		Objects.requireNonNull(operator, "Comparison operator cannot be null.");

		if (groupByColumns.isEmpty()) {
			throw new IllegalArgumentException("Group-by columns list cannot be empty.");
		}
		if (aggregationColumn.trim().isEmpty()) {
			throw new IllegalArgumentException("Aggregation column name cannot be blank.");
		}
		if (groupByColumns.contains(aggregationColumn)) {
			throw new IllegalArgumentException("The aggregation column cannot be one of the group-by columns.");
		}

		this.groupByColumns = Collections.unmodifiableList(new ArrayList<>(groupByColumns));
		this.aggregationColumn = aggregationColumn;
		this.function = function;
		this.operator = operator;
		this.threshold = threshold;
	}

	public List<String> getGroupByColumns() {
		return groupByColumns;
	}

	public String getAggregationColumn() {
		return aggregationColumn;
	}

	public AggregationFunction getFunction() {
		return function;
	}

	public ComparisonOperator getOperator() {
		return operator;
	}

	public double getThreshold() {
		return threshold;
	}

	@Override
	public boolean accept(RuleValidationVisitor visitor, Row row, List<Row> allRows) {
		return visitor.visit(this, row, allRows);
	}
}
