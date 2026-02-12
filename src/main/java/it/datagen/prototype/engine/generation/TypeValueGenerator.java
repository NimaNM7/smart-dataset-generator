package it.datagen.prototype.engine.generation;

import com.github.javafaker.Faker;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates one sample value for a given Java type.
 */
public class TypeValueGenerator {

	private final Faker faker;
	private final Random random;

	public TypeValueGenerator() {
		this(new Faker(), ThreadLocalRandom.current());
	}

	public TypeValueGenerator(Faker faker, Random random) {
		this.faker = faker;
		this.random = random;
	}

	public Object generateValue(Class<?> rawType) {
		Class<?> type = normalizeType(rawType);

		if (type == String.class) {
			return faker.lorem().word();
		}
		if (type == Integer.class) {
			return random.nextInt(10_000);
		}
		if (type == Long.class) {
			return Math.abs(random.nextLong() % 1_000_000L);
		}
		if (type == Double.class) {
			return random.nextDouble() * 10_000.0d;
		}
		if (type == Float.class) {
			return random.nextFloat() * 10_000.0f;
		}
		if (type == Short.class) {
			return (short) random.nextInt(Short.MAX_VALUE + 1);
		}
		if (type == Byte.class) {
			return (byte) random.nextInt(Byte.MAX_VALUE + 1);
		}
		if (type == Boolean.class) {
			return random.nextBoolean();
		}
		if (type == Character.class) {
			return faker.lorem().character();
		}
		if (type == BigDecimal.class) {
			return BigDecimal.valueOf(random.nextDouble() * 10_000.0d).setScale(2, RoundingMode.HALF_UP);
		}
		if (type == BigInteger.class) {
			return BigInteger.valueOf(Math.abs(random.nextLong() % 1_000_000L));
		}
		if (type == Date.class) {
			return faker.date().birthday();
		}
		if (type == Instant.class) {
			return Instant.ofEpochMilli(faker.date().birthday().getTime());
		}
		if (type == LocalDate.class) {
			return faker.date().birthday().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
		}
		if (type == LocalDateTime.class) {
			return faker.date().birthday().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
		}
		if (type == UUID.class) {
			return UUID.randomUUID();
		}
		if (type == byte[].class) {
			byte[] value = new byte[8];
			random.nextBytes(value);
			return value;
		}
		if (type.isEnum()) {
			Object[] constants = type.getEnumConstants();
			if (constants.length == 0) {
				throw new IllegalArgumentException("Enum has no constants: " + type.getName());
			}
			return constants[random.nextInt(constants.length)];
		}

		throw new IllegalArgumentException("Unsupported column type for generation: " + type.getName());
	}

	private Class<?> normalizeType(Class<?> type) {
		if (!type.isPrimitive()) {
			return type;
		}
		if (type == int.class) {
			return Integer.class;
		}
		if (type == long.class) {
			return Long.class;
		}
		if (type == double.class) {
			return Double.class;
		}
		if (type == float.class) {
			return Float.class;
		}
		if (type == short.class) {
			return Short.class;
		}
		if (type == byte.class) {
			return Byte.class;
		}
		if (type == boolean.class) {
			return Boolean.class;
		}
		if (type == char.class) {
			return Character.class;
		}
		return type;
	}
}
