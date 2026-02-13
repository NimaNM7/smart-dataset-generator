package it.datagen.prototype;

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

public class BenchmarkMain {

	private static final int ROW_COUNT = 10000;
	private static final int BENCHMARK_RUNS = 1;
	private static final Path OUTPUT_DIR = Path.of("output");

	public static void main(String[] args) throws IOException {
		TableSchema schema = buildSchema();
		List<DataQualityRule> rules = buildRules();
		System.out.println("columns: " + schema.getColumns().size());
		System.out.println("rules: " + rules.size());
		printStages(rules);

		DatasetGeneratorV1 generatorV1 = new DatasetGeneratorV1();
		DatasetGeneratorV2 generatorV2 = new DatasetGeneratorV2();
		DatasetValidator validator = new DatasetValidator();

		BenchmarkResult v1Result = benchmark("DatasetGeneratorV1",
				() -> generatorV1.generateTable(schema, rules, ROW_COUNT));
		BenchmarkResult v2Result = benchmark("DatasetGeneratorV2",
				() -> generatorV2.generateTable(schema, rules, ROW_COUNT));

		boolean v1Valid = validator.validate(schema, v1Result.rows(), rules);
		boolean v2Valid = validator.validate(schema, v2Result.rows(), rules);

		Files.createDirectories(OUTPUT_DIR);
		writeCsv(schema, v1Result.rows(), OUTPUT_DIR.resolve("dataset_v1.csv"));
		writeCsv(schema, v2Result.rows(), OUTPUT_DIR.resolve("dataset_v2.csv"));

		printResult(v1Result, v1Valid, OUTPUT_DIR.resolve("dataset_v1.csv"));
		printResult(v2Result, v2Valid, OUTPUT_DIR.resolve("dataset_v2.csv"));
	}

	private static TableSchema buildSchema() {
		List<Column> columns = List.of(
				new Column("region", String.class),
				new Column("city", String.class),
				new Column("shortCode", String.class),
				new Column("country", String.class),
				new Column("startDate", LocalDate.class),
				new Column("endDate", LocalDate.class),
				new Column("approvalDate", LocalDate.class),
				new Column("baseAmount", Integer.class),
				new Column("discountedAmount", Integer.class),
				new Column("limitAmount", Integer.class)
		);
		return new TableSchema("demo_dataset", columns);
	}

	private static List<DataQualityRule> buildRules() {
		return List.of(
				// SCC #1 (Strings): region -> city -> shortCode -> region
				new FunctionalDependencyRule(List.of("region"), "city"),
				new FunctionalDependencyRule(List.of("city"), "shortCode"),
				new LengthComparisonRule("shortCode", "region",
						LengthComparisonRule.ComparisonType.SHORTER_THAN_OR_EQUAL),

				new FunctionalDependencyRule(List.of("city"), "country"),

				// SCC #2 (Dates): startDate -> endDate -> approvalDate -> startDate
				new TemporalOrderRule("startDate", "endDate", true),
				new TemporalOrderRule("endDate", "approvalDate", true),
				new TemporalOrderRule("approvalDate", "startDate", true),

				// SCC #3 (Numbers): discounted -> base -> limit -> discounted
				new ColumnComparisonRule("discountedAmount", "baseAmount", true),
				new ColumnComparisonRule("baseAmount", "limitAmount", true),
				new ColumnComparisonRule("limitAmount", "discountedAmount", true),

				new ConditionalCompletenessRule("city", "veritatis", "discountedAmount", false)
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
		List<Long> runDurationsNanos = java.util.stream.IntStream.range(0, BenchmarkMain.BENCHMARK_RUNS)
				.mapToObj(i -> timedRun(i, generator))
				.peek(result -> System.out.println(name + " run#" + (result.runIndex() + 1) + " = "
						+ toMillis(result.durationNanos()) + " ms"))
				.map(TimedRunResult::durationNanos)
				.toList();

		List<Row> rows = generator.get();
		long averageNanos = (long) runDurationsNanos.stream().mapToLong(Long::longValue).average().orElse(0.0d);
		long minNanos = runDurationsNanos.stream().mapToLong(Long::longValue).min().orElse(0L);
		long maxNanos = runDurationsNanos.stream().mapToLong(Long::longValue).max().orElse(0L);
		long usedMemoryBytes = usedHeapBytes();

		return new BenchmarkResult(name, rows, averageNanos, minNanos, maxNanos, usedMemoryBytes);
	}

	private static TimedRunResult timedRun(int runIndex, Supplier<List<Row>> generator) {
		long start = System.nanoTime();
		generator.get();
		long duration = System.nanoTime() - start;
		return new TimedRunResult(runIndex, duration);
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

	private record TimedRunResult(int runIndex, long durationNanos) {
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
