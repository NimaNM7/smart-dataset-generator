package it.datagen.prototype.model.rules;

import it.datagen.prototype.engine.dependencies.DependencyGraphVisitor;

public interface DependencyAwareRule {
    void accept(DependencyGraphVisitor visitor);
}
