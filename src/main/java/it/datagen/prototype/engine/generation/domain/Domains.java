package it.datagen.prototype.engine.generation.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Factory for default domains based on Java types.
 */
public final class Domains {

	private Domains() {
	}

	public static Domain<?> defaultDomainFor(Class<?> rawType) {
		Objects.requireNonNull(rawType, "rawType");
		Class<?> type = normalizeType(rawType);

		if (type == String.class) {
			return new StringDomain(1, 32);
		}
		if (type == Integer.class) {
			return new IntDomain(0, 1_000_000);
		}
		if (type == Long.class) {
			return new LongDomain(0L, 1_000_000L);
		}
		if (type == Double.class) {
			return new DoubleDomain(0.0d, 10_000.0d);
		}
		if (type == Float.class) {
			return new DoubleDomain(0.0d, 10_000.0d);
		}
		if (type == Short.class) {
			return new IntDomain(0, Short.MAX_VALUE);
		}
		if (type == Byte.class) {
			return new IntDomain(0, Byte.MAX_VALUE);
		}
		if (type == Boolean.class) {
			return BooleanDomain.any();
		}
		if (type == Character.class) {
			return new StringDomain(1, 1);
		}
		if (type == BigDecimal.class) {
			return new BigDecimalDomain(BigDecimal.ZERO, BigDecimal.valueOf(10_000.0d), 2);
		}
		if (type == java.math.BigInteger.class) {
			return new LongDomain(0L, 1_000_000L);
		}
		if (type == java.util.Date.class) {
			return new LongDomain(0L, System.currentTimeMillis());
		}
		if (type == java.time.Instant.class) {
			return new LongDomain(0L, System.currentTimeMillis());
		}
		if (type == LocalDate.class) {
			return new LocalDateDomain(LocalDate.of(1970, 1, 1), LocalDate.now());
		}
		if (type == java.time.LocalDateTime.class) {
			return new LongDomain(0L, System.currentTimeMillis());
		}
		if (type == UUID.class) {
			return new UuidDomain();
		}
		if (type == byte[].class) {
			return new BytesDomain(8, 8);
		}
		if (type.isEnum()) {
			@SuppressWarnings("unchecked")
			Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
			return EnumDomain.any(enumType);
		}

		throw new IllegalArgumentException("Unsupported column type for domain: " + type.getName());
	}

	private static Class<?> normalizeType(Class<?> type) {
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
