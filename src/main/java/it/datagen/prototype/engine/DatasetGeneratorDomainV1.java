package it.datagen.prototype.engine;

import com.github.javafaker.Faker;
import it.datagen.prototype.engine.generation.RowGenerationEngine;
import it.datagen.prototype.engine.generation.SamplingContext;
import it.datagen.prototype.engine.models.ExecutionPlan;
import it.datagen.prototype.engine.models.RuleProcessingContext;
import it.datagen.prototype.engine.planner.RulePlanner;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.TableSchema;
import it.datagen.prototype.model.rules.DataQualityRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Domain-first dataset generator (V1 of the new architecture).
 */
public final class DatasetGeneratorDomainV1 {

	private final RulePlanner planner;
	private final SamplingContext samplingContext;
	private final int maxPropagationPasses;
	private final int maxSccRetries;

	public DatasetGeneratorDomainV1() {
		this(new RulePlanner(), new SamplingContext(new Faker(), new Random()), 5, 10);
	}

	public DatasetGeneratorDomainV1(RulePlanner planner, SamplingContext samplingContext,
			int maxPropagationPasses, int maxSccRetries) {
		this.planner = Objects.requireNonNull(planner, "planner");
		this.samplingContext = Objects.requireNonNull(samplingContext, "samplingContext");
		this.maxPropagationPasses = Math.max(1, maxPropagationPasses);
		this.maxSccRetries = Math.max(1, maxSccRetries);
	}

	public List<Row> generateTable(TableSchema schema, List<DataQualityRule> rules, int rowCount) {
		Objects.requireNonNull(schema, "schema");
		Objects.requireNonNull(rules, "rules");
		if (rowCount < 0) {
			throw new IllegalArgumentException("rowCount must be >= 0");
		}

		ExecutionPlan plan = planner.plan(schema.getColumns(), rules);
		RowGenerationEngine engine = new RowGenerationEngine(plan, maxPropagationPasses, maxSccRetries);
		RuleProcessingContext context = new RuleProcessingContext();

		List<Row> rows = new ArrayList<>(rowCount);
		for (int i = 0; i < rowCount; i++) {
			rows.add(new Row(engine.generateRow(samplingContext, context)));
		}
		return rows;
	}
}

