package it.datagen.prototype.engine.generation;

import com.github.javafaker.Faker;

import java.util.Objects;
import java.util.Random;

public final class SamplingContext {

	private final Faker faker;
	private final Random random;

	public SamplingContext(Faker faker, Random random) {
		this.faker = Objects.requireNonNull(faker, "faker");
		this.random = Objects.requireNonNull(random, "random");
	}

	public Faker faker() {
		return faker;
	}

	public Random random() {
		return random;
	}
}

