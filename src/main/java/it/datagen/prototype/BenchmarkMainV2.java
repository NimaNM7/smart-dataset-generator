package it.datagen.prototype;

import it.datagen.prototype.engine.DatasetGeneratorDomainV1;
import it.datagen.prototype.engine.DatasetGeneratorV1;
import it.datagen.prototype.engine.DatasetGeneratorV2;
import it.datagen.prototype.engine.DatasetValidator;
import it.datagen.prototype.engine.dependencies.DependencySolver;
import it.datagen.prototype.model.Column;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.TableSchema;
import it.datagen.prototype.model.rules.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * BenchmarkMainV2: Sales Order Domain with Supervisory Hierarchy.
 *
 * Uses rules not covered in BenchmarkMain:
 * - AggregationRule (sum constraints across rows)
 * - IntraTableParentRule (hierarchical constraints within same table)
 * - More complex FunctionalDependencies
 *
 * Domain: Sales Order Processing with Manager-Employee Hierarchy
 */
public class BenchmarkMainV2 {

    private static final int ROW_COUNT = 10000;
    private static final int BENCHMARK_RUNS = 1;
    private static final Path OUTPUT_DIR = Path.of("output");

    public static void main(String[] args) throws IOException {
        TableSchema schema = buildSchema();
        List<DataQualityRule> rules = buildRules();
        System.out.println("=== BenchmarkMainV2: Sales Order Domain ===");
        System.out.println("columns: " + schema.getColumns().size());
        System.out.println("rules: " + rules.size());
        printStages(rules);

        DatasetGeneratorDomainV1 generatorDomainV1 = new DatasetGeneratorDomainV1();
        DatasetValidator validator = new DatasetValidator();

        BenchmarkResult domainV1Result = benchmark("DatasetGeneratorDomainV1",
                () -> generatorDomainV1.generateTable(schema, rules, ROW_COUNT));

        boolean domainV1Valid = validator.validate(schema, domainV1Result.rows(), rules);

        Files.createDirectories(OUTPUT_DIR);
        writeCsv(schema, domainV1Result.rows(), OUTPUT_DIR.resolve("dataset_domain_v1_v2.csv"));
        printResult(domainV1Result, domainV1Valid, OUTPUT_DIR.resolve("dataset_domain_v1_v2.csv"));
    }

    /**
     * Sales Order Schema with Supervisory Hierarchy.
     *
     * Columns represent order data with employee-supervisor relationships.
     */
    private static TableSchema buildSchema() {
        List<Column> columns = List.of(
                // Order metadata
                new Column("orderIndex", Integer.class),  // Sequence for unique ID generation
                new Column("orderId", String.class),
                new Column("orderDate", LocalDate.class),
                new Column("shipDate", LocalDate.class),

                // Salesperson hierarchy
                new Column("salespersonId", String.class),
                new Column("supervisorId", String.class), // References another salesperson

                // Product information
                new Column("productCode", String.class),
                new Column("productCategory", String.class),

                // Order amounts
                new Column("unitPrice", Integer.class),
                new Column("quantity", Integer.class),
                new Column("subtotal", Integer.class),
                new Column("discountAmount", Integer.class),
                new Column("totalAmount", Integer.class),

                // Quotas and achievements (hierarchical)
                new Column("salespersonQuota", Integer.class),
                new Column("salespersonAchieved", Integer.class),
                new Column("supervisorQuota", Integer.class),
                new Column("supervisorAchieved", Integer.class)
        );
        return new TableSchema("sales_orders", columns);
    }

    /**
     * Rules for Sales Order Domain.
     *
     * Includes:
     * - FunctionalDependencies for product and employee hierarchy
     * - ColumnComparisons for order amounts
     * - TemporalOrder for order/ship dates
     * - AggregationRules for quotas (NEW - not in BenchmarkMain)
     * - IntraTableParentRule for supervisory hierarchy (NEW - not in BenchmarkMain)
     * - UniqueKeyRule for order IDs
     * - ConditionalCompletenessRule for premium products
     */
    private static List<DataQualityRule> buildRules() {
        return List.of(
                // === Unique Order Generation: orderIndex → orderId
                // Each orderIndex maps to a unique orderId (ensures cardinality)
                new FunctionalDependencyRule(List.of("orderIndex"), "orderId"),

                // === Dependency Chain: productCode → productCategory
                new FunctionalDependencyRule(List.of("productCode"), "productCategory"),

                // === Dependency Chain: salespersonId → supervisorId
                // (salesperson references their supervisor in the same table)
                new FunctionalDependencyRule(List.of("salespersonId"), "supervisorId"),

                // === Order Amount Constraints (Numeric SCC)
                // discountAmount <= subtotal
                new ColumnComparisonRule("discountAmount", "subtotal", true),

                // totalAmount <= subtotal (after discount is applied)
                new ColumnComparisonRule("totalAmount", "subtotal", true),

                // === Date Order Constraints
                // Order must be placed before shipment
                new TemporalOrderRule("orderDate", "shipDate", true),

                // === String Length Constraint
                // Product code should be longer than or equal to category name
                new LengthComparisonRule("productCode", "productCategory",
                        LengthComparisonRule.ComparisonType.LONGER_THAN_OR_EQUAL),

                // === Aggregation Constraints (NEW - not in BenchmarkMain)
                // Salesperson's total sales per organization cannot exceed quota
                // sum(totalAmount) per salespersonId <= salespersonQuota (5000)
                new AggregationRule(
                        List.of("salespersonId"),
                        "totalAmount",
                        AggregationRule.AggregationFunction.SUM,
                        AggregationRule.ComparisonOperator.LESS_THAN_OR_EQUAL,
                        5000.0
                ),

                // Product category inventory limit: sum(quantity) per category <= 5000
                new AggregationRule(
                        List.of("productCategory"),
                        "quantity",
                        AggregationRule.AggregationFunction.SUM,
                        AggregationRule.ComparisonOperator.LESS_THAN_OR_EQUAL,
                        5000.0
                ),

                // === Unique Key Constraint
                // Order IDs must be globally unique
                new UniqueKeyRule(List.of("orderId")),

                // === Hierarchical/Intra-Table Parent Constraints (NEW - not in BenchmarkMain)
                // IntraTableParentRule only defines the foreign key relationship
                // (salespersonId references supervisorId in the same table)
                new IntraTableParentRule("salespersonId", "supervisorId"),

                // Additional hierarchical constraints via ColumnComparison rules
                // Salesperson's achieved sales <= Supervisor's achieved sales
                new ColumnComparisonRule("salespersonAchieved", "supervisorAchieved", true),

                // Salesperson's quota <= Supervisor's quota
                new ColumnComparisonRule("salespersonQuota", "supervisorQuota", true),

                // === Conditional Completeness
                // If product is in premium category, minimum unit price is high
                new ConditionalCompletenessRule(
                        "productCategory",     // condition column
                        "premium",             // condition value
                        "unitPrice",           // target column
                        false                  // not nullable when condition is true
                )
        );
    }

    private static void printStages(List<DataQualityRule> rules) {
        List<Set<String>> stages = new DependencySolver().solveTopologically(rules);
        System.out.println("SCC stages (" + stages.size() + "):");
        for (int i = 0; i < stages.size(); i++) {
            Set<String> stage = stages.get(i);
            System.out.println("  stage#" + (i + 1) + " size=" + stage.size() + " -> " + stage);
        }
        System.out.println();
    }

    private static BenchmarkResult benchmark(String name, Supplier<List<Row>> generator) {
        long startMemory = usedHeapBytes();
        long start = System.nanoTime();
        List<Row> rows = generator.get();
        long duration = System.nanoTime() - start;
        long endMemory = usedHeapBytes();

        System.out.println(name + " = " + toMillis(duration) + " ms, rows = " + rows.size());

        return new BenchmarkResult(name, rows, duration, duration, duration, endMemory - startMemory);
    }

    private static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static double toMillis(long nanos) {
        return BigDecimal.valueOf(nanos).divide(BigDecimal.valueOf(1_000_000L), 3, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static void printResult(BenchmarkResult result, boolean valid, Path csvPath) {
        System.out.println();
        System.out.println("=== " + result.name() + " ===");
        System.out.println("rows: " + result.rows().size());
        System.out.println("valid: " + valid);
        System.out.println("avg(ms): " + toMillis(result.averageNanos()));
        System.out.println("min(ms): " + toMillis(result.minNanos()));
        System.out.println("max(ms): " + toMillis(result.maxNanos()));
        System.out.println("heap(bytes): " + result.usedHeapBytes());
        System.out.println("csv: " + csvPath.toAbsolutePath());
    }

    private static void writeCsv(TableSchema schema, List<Row> rows, Path outputFile) throws IOException {
        List<String> headers = schema.getColumns().stream().map(Column::getName).collect(Collectors.toList());
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        joiner.add(String.join(",", headers));

        for (Row row : rows) {
            String line = headers.stream()
                    .map(columnName -> escapeCsv(toCsvCell(row.getValue(columnName))))
                    .collect(Collectors.joining(","));
            joiner.add(line);
        }

        Files.writeString(outputFile, joiner.toString());
    }

    private static String toCsvCell(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof byte[]) {
            return Arrays.toString((byte[]) value);
        }
        return String.valueOf(value);
    }

    private static String escapeCsv(String raw) {
        if (raw == null) {
            return "";
        }
        boolean mustQuote = raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r");
        if (!mustQuote) {
            return raw;
        }
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    private record BenchmarkResult(
            String name,
            List<Row> rows,
            long averageNanos,
            long minNanos,
            long maxNanos,
            long usedHeapBytes
    ) {
    }
}
