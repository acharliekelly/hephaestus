package me.acharliekelly.hephaestus.web;

import java.time.Instant;
import me.acharliekelly.hephaestus.model.RepositoryRecord;

public record RepositoryResponse(
        Long id,
        String name,
        String canonicalPath,
        Instant importedAt
) {
    public static RepositoryResponse from(RepositoryRecord repository) {
        return new RepositoryResponse(
                repository.getId(),
                repository.getName(),
                repository.getCanonicalPath(),
                repository.getImportedAt()
        );
    }
}

