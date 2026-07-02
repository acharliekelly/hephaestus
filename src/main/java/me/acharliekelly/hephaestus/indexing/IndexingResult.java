package me.acharliekelly.hephaestus.indexing;

public record IndexingResult(
        Long repositoryId,
        int sourceFileCount,
        int symbolCount,
        int dependencyCount
) {
}

