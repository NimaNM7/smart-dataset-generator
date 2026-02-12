package it.datagen.prototype.engine.dependencies.handlers;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

final class DependencyGraphUtils {

	private DependencyGraphUtils() {
	}

	static void addDependency(Graph<String, DefaultEdge> graph, String sourceColumn, String targetColumn) {
		graph.addVertex(sourceColumn);
		graph.addVertex(targetColumn);
		graph.addEdge(sourceColumn, targetColumn);
	}
}
