package me.acharliekelly.hephaestus.graph;

public record SymbolDependencySummaryResponse(
        String fromQualifiedName,
        String toQualifiedName,
        long dependencyCount
) {
}

