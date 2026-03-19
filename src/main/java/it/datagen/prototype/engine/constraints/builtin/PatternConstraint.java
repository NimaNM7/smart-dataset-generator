package it.datagen.prototype.engine.constraints.builtin;

import it.datagen.prototype.engine.constraints.GenerationConstraint;
import it.datagen.prototype.engine.generation.RowGenerationState;
import it.datagen.prototype.engine.generation.domain.Domain;
import it.datagen.prototype.engine.generation.domain.StringDomain;
import it.datagen.prototype.model.rules.PatternRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pattern constraint: enforces that a column value matches a regular expression.
 *
 * <p>Generation-time strategy: When the column has no value yet, generate a regex-matching
 * string directly and force it into the state, narrowing the StringDomain to a singleton.
 */
public final class PatternConstraint {

	private PatternConstraint() {}

	public static GenerationConstraint fromRule(PatternRule rule) {
		Objects.requireNonNull(rule, "rule");

		String columnName = rule.getColumnName();
		Pattern pattern = rule.getPattern();

		return new GenerationConstraint() {
			@Override
			public Set<String> involvedColumns() {
				return Set.of(columnName);
			}

			@Override
			public boolean narrow(RowGenerationState state) {
				// Only act if the column has no value yet
				if (state.values().containsKey(columnName)) {
					return false;
				}

				// Generate a string matching the pattern
				String generated = generateFromRegex(pattern.pattern(), state.samplingContext().random());
				if (generated == null) {
					return false;
				}

				// Force the value into the state
				state.values().put(columnName, generated);

				// Narrow the StringDomain to singleton
				Domain<?> domain = state.domainFor(columnName);
				if (domain instanceof StringDomain sd) {
					sd.allowOnly(generated);
				}

				return true;
			}
		};
	}

	/**
	 * Generates a string matching the given regex pattern.
	 *
	 * <p>Supports:
	 * - Character classes: [A-Z], [a-z], [0-9], combined like [A-Za-z0-9]
	 * - Quantifiers: {n} (exact) and {n,m} (range, pick random count)
	 * - Escape sequences: \d (digit), \w (word char), \s (space)
	 * - Literal characters
	 *
	 * @param regex the regular expression pattern
	 * @param random random number generator
	 * @return a generated string matching the pattern, or null if unable
	 */
	private static String generateFromRegex(String regex, Random random) {
		try {
			StringBuilder result = new StringBuilder();
			int i = 0;
			while (i < regex.length()) {
				char ch = regex.charAt(i);

				if (ch == '[') {
					// Character class
					int end = regex.indexOf(']', i);
					if (end == -1) {
						return null; // invalid
					}
					String classContent = regex.substring(i + 1, end);
					char selected = selectFromCharClass(classContent, random);
					result.append(selected);
					i = end + 1;
				} else if (ch == '\\') {
					// Escape sequence
					if (i + 1 >= regex.length()) {
						return null;
					}
					char next = regex.charAt(i + 1);
					if (next == 'd') {
						result.append((char) ('0' + random.nextInt(10)));
						i += 2;
					} else if (next == 'w') {
						// word char: [A-Za-z0-9_]
						String wordChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
						result.append(wordChars.charAt(random.nextInt(wordChars.length())));
						i += 2;
					} else if (next == 's') {
						result.append(' ');
						i += 2;
					} else {
						// literal character after backslash
						result.append(next);
						i += 2;
					}
				} else if (ch == '{') {
					// Quantifier
					int end = regex.indexOf('}', i);
					if (end == -1) {
						return null;
					}
					String quantContent = regex.substring(i + 1, end);
					int count = parseQuantifier(quantContent, random);
					if (count < 0) {
						return null;
					}
					// Repeat the previous character/class 'count' times
					if (result.length() == 0) {
						return null;
					}
					char last = result.charAt(result.length() - 1);
					for (int j = 1; j < count; j++) {
						result.append(last);
					}
					i = end + 1;
				} else if (ch == '(' || ch == ')' || ch == '*' || ch == '+' || ch == '?' || ch == '|' || ch == '^' || ch == '$' || ch == '.') {
					// Complex regex features we don't support
					return null;
				} else {
					// Literal character
					result.append(ch);
					i++;
				}
			}
			return result.toString();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Selects a character from a character class.
	 *
	 * <p>Supports ranges like A-Z, a-z, 0-9 and unions like [A-Za-z0-9].
	 *
	 * @param classContent the content between [ and ] (e.g., "A-Z" or "A-Za-z0-9")
	 * @param random random number generator
	 * @return a randomly selected character from the class
	 */
	private static char selectFromCharClass(String classContent, Random random) {
		List<Character> chars = new ArrayList<>();

		int i = 0;
		while (i < classContent.length()) {
			if (i + 2 < classContent.length() && classContent.charAt(i + 1) == '-') {
				// Range
				char from = classContent.charAt(i);
				char to = classContent.charAt(i + 2);
				for (char c = from; c <= to; c++) {
					chars.add(c);
				}
				i += 3;
			} else {
				chars.add(classContent.charAt(i));
				i++;
			}
		}

		if (chars.isEmpty()) {
			return 'X';
		}
		return chars.get(random.nextInt(chars.size()));
	}

	/**
	 * Parses a quantifier like "3" or "3,5".
	 *
	 * @param quantContent the content between { and } (e.g., "3" or "3,5")
	 * @param random random number generator
	 * @return the number of repetitions, or -1 if unable to parse
	 */
	private static int parseQuantifier(String quantContent, Random random) {
		if (quantContent.contains(",")) {
			String[] parts = quantContent.split(",");
			if (parts.length != 2) {
				return -1;
			}
			try {
				int min = Integer.parseInt(parts[0].trim());
				int max = Integer.parseInt(parts[1].trim());
				return min + random.nextInt(max - min + 1);
			} catch (NumberFormatException e) {
				return -1;
			}
		} else {
			try {
				return Integer.parseInt(quantContent.trim());
			} catch (NumberFormatException e) {
				return -1;
			}
		}
	}
}
