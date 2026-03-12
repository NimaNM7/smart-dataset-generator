package it.datagen.prototype.engine.generation;

import it.datagen.prototype.engine.generation.domain.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Samples concrete values from domains.
 */
public final class SamplerRegistry {

	private SamplerRegistry() {
	}

	public static Object sample(Domain<?> domain, SamplingContext context) {
		Objects.requireNonNull(domain, "domain");
		Objects.requireNonNull(context, "context");
		if (domain.isEmpty()) {
			throw new IllegalStateException("Cannot sample from empty domain: " + domain.kind());
		}

		return switch (domain.kind()) {
			case INT -> sampleInt((IntDomain) domain, context);
			case LONG -> sampleLong((LongDomain) domain, context);
			case DOUBLE -> sampleDouble((DoubleDomain) domain, context);
			case BIG_DECIMAL -> sampleBigDecimal((BigDecimalDomain) domain, context);
			case LOCAL_DATE -> sampleLocalDate((LocalDateDomain) domain, context);
			case STRING -> sampleString((StringDomain) domain, context);
			case BOOLEAN -> sampleBoolean((BooleanDomain) domain, context);
			case ENUM -> sampleEnum((EnumDomain) domain, context);
			case UUID -> UUID.randomUUID();
			case BYTES -> sampleBytes((BytesDomain) domain, context);
		};
	}

	private static int sampleInt(IntDomain domain, SamplingContext context) {
		int min = domain.min();
		int max = domain.max();
		if (min == max) {
			return min;
		}
		return min + context.random().nextInt((max - min) + 1);
	}

	private static long sampleLong(LongDomain domain, SamplingContext context) {
		long min = domain.min();
		long max = domain.max();
		if (min == max) {
			return min;
		}
		long bound = (max - min) + 1;
		long next = Math.floorMod(context.random().nextLong(), bound);
		return min + next;
	}

	private static double sampleDouble(DoubleDomain domain, SamplingContext context) {
		double min = domain.min();
		double max = domain.max();
		if (Double.compare(min, max) == 0) {
			return min;
		}
		return min + (context.random().nextDouble() * (max - min));
	}

	private static BigDecimal sampleBigDecimal(BigDecimalDomain domain, SamplingContext context) {
		BigDecimal min = domain.min();
		BigDecimal max = domain.max();
		if (min.compareTo(max) == 0) {
			return min.setScale(domain.scale(), RoundingMode.HALF_UP);
		}
		double value = min.doubleValue() + (context.random().nextDouble() * (max.doubleValue() - min.doubleValue()));
		return BigDecimal.valueOf(value).setScale(domain.scale(), RoundingMode.HALF_UP);
	}

	private static LocalDate sampleLocalDate(LocalDateDomain domain, SamplingContext context) {
		LocalDate min = domain.min();
		LocalDate max = domain.max();
		if (min.equals(max)) {
			return min;
		}
		long startDay = min.toEpochDay();
		long endDay = max.toEpochDay();
		long day = startDay + Math.floorMod(context.random().nextLong(), (endDay - startDay) + 1);
		return LocalDate.ofEpochDay(day);
	}

	private static String sampleString(StringDomain domain, SamplingContext context) {
		if (domain.allowedValues() != null && !domain.allowedValues().isEmpty()) {
			int index = context.random().nextInt(domain.allowedValues().size());
			return domain.allowedValues().get(index);
		}

		int minLen = domain.minLength();
		int maxLen = domain.maxLength();
		int targetLen = minLen == maxLen ? minLen : (minLen + context.random().nextInt((maxLen - minLen) + 1));
		String base = context.faker().lorem().word();
		if (base.length() == targetLen) {
			return base;
		}
		if (base.length() > targetLen) {
			return base.substring(0, targetLen);
		}
		StringBuilder builder = new StringBuilder(base);
		while (builder.length() < targetLen) {
			builder.append('_');
		}
		return builder.toString();
	}

	private static boolean sampleBoolean(BooleanDomain domain, SamplingContext context) {
		if (domain.allowsTrue() && domain.allowsFalse()) {
			return context.random().nextBoolean();
		}
		return domain.allowsTrue();
	}

	private static Object sampleEnum(EnumDomain domain, SamplingContext context) {
		return domain.allowed().get(context.random().nextInt(domain.allowed().size()));
	}

	private static byte[] sampleBytes(BytesDomain domain, SamplingContext context) {
		int minLen = domain.minLength();
		int maxLen = domain.maxLength();
		int len = minLen == maxLen ? minLen : (minLen + context.random().nextInt((maxLen - minLen) + 1));
		byte[] value = new byte[len];
		context.random().nextBytes(value);
		return value;
	}
}
