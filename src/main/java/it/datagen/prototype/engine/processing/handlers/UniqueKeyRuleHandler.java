package it.datagen.prototype.engine.processing.handlers;

import it.datagen.prototype.engine.processing.RuleHandler;
import it.datagen.prototype.engine.processing.RuleProcessingContext;
import it.datagen.prototype.model.Row;
import it.datagen.prototype.model.rules.UniqueKeyRule;

import java.util.ArrayList;
import java.util.List;

public class UniqueKeyRuleHandler implements RuleHandler<UniqueKeyRule> {

	@Override
	public Class<UniqueKeyRule> ruleType() {
		return UniqueKeyRule.class;
	}

	@Override
	public boolean validate(UniqueKeyRule rule, Row row, List<Row> allRows, RuleProcessingContext context) {
		if (rule == null || row == null || context == null) {
			return false;
		}

		List<Object> keyValues = new ArrayList<>(rule.getColumnNames().size());
		for (String columnName : rule.getColumnNames()) {
			keyValues.add(row.getValue(columnName));
		}
		return context.registerUniqueKey(rule, keyValues);
	}
}
