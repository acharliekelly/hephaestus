package me.acharliekelly.hephaestus.graph;

import java.util.List;
import java.util.Map;

public record ArchitectureSummaryResponse(
        Long repositoryId,
        int sourceFileCount,
        int packageCount,
        Map<String, Long> symbolCounts,
        Map<String, Long> dependencyCounts,
        List<PackageDependencyResponse> packageDependencies,
        List<SymbolDependencySummaryResponse> classDependencies,
        List<SymbolDependencySummaryResponse> topDependedOnSymbols
) {
}

