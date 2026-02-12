package it.datagen.prototype.engine.processing.handlers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

final class RuleValueUtils {

	private RuleValueUtils() {
	}

	static Integer compareValues(Object left, Object right) {
		if (left == null || right == null) {
			return null;
		}

		if (left instanceof Number && right instanceof Number) {
			BigDecimal leftNumber = new BigDecimal(left.toString());
			BigDecimal rightNumber = new BigDecimal(right.toString());
			return leftNumber.compareTo(rightNumber);
		}

		if (left instanceof Date && right instanceof Date) {
			return Long.compare(((Date) left).getTime(), ((Date) right).getTime());
		}

		if (left instanceof Comparable && left.getClass().isInstance(right)) {
			@SuppressWarnings("unchecked")
			Comparable<Object> comparableLeft = (Comparable<Object>) left;
			return comparableLeft.compareTo(right);
		}

		return null;
	}

	static Object incrementStrictly(Object value) {
		if (value instanceof Integer) {
			return ((Integer) value) + 1;
		}
		if (value instanceof Long) {
			return ((Long) value) + 1L;
		}
		if (value instanceof Short) {
			return (short) (((Short) value) + 1);
		}
		if (value instanceof Byte) {
			return (byte) (((Byte) value) + 1);
		}
		if (value instanceof Double) {
			return ((Double) value) + 1.0d;
		}
		if (value instanceof Float) {
			return ((Float) value) + 1.0f;
		}
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).add(BigDecimal.ONE);
		}
		if (value instanceof BigInteger) {
			return ((BigInteger) value).add(BigInteger.ONE);
		}
		if (value instanceof Date) {
			return new Date(((Date) value).getTime() + 1L);
		}
		if (value instanceof LocalDate) {
			return ((LocalDate) value).plusDays(1);
		}
		if (value instanceof LocalDateTime) {
			return ((LocalDateTime) value).plusNanos(1);
		}
		if (value instanceof Instant) {
			return ((Instant) value).plusMillis(1);
		}
		if (value instanceof String) {
			return value + "_";
		}
		return null;
	}
}
