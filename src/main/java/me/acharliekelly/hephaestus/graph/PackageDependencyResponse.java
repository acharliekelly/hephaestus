package me.acharliekelly.hephaestus.graph;

public record PackageDependencyResponse(
        String fromPackage,
        String toPackage,
        long dependencyCount
) {
}

