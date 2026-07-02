package me.acharliekelly.hephaestus.repo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import me.acharliekelly.hephaestus.model.RepositoryRecord;
import me.acharliekelly.hephaestus.model.persistence.RepositoryRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepoService {
    private final RepositoryRecordRepository repositories;

    public RepoService(RepositoryRecordRepository repositories) {
        this.repositories = repositories;
    }

    @Transactional
    public RepositoryRecord importLocal(String rawPath) {
        Path canonicalPath = validateLocalPath(rawPath);
        String canonicalPathString = canonicalPath.toString();
        return repositories.findByCanonicalPath(canonicalPathString)
                .orElseGet(() -> repositories.save(new RepositoryRecord(
                        canonicalPath.getFileName().toString(),
                        canonicalPathString,
                        Instant.now()
                )));
    }

    public RepositoryRecord requireRepository(Long repositoryId) {
        return repositories.findById(repositoryId)
                .orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " was not found"));
    }

    private Path validateLocalPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new InvalidRepositoryPathException("Path is required");
        }
        Path path = Path.of(rawPath);
        if (!path.isAbsolute()) {
            throw new InvalidRepositoryPathException("Path must be absolute");
        }
        try {
            Path canonicalPath = path.toRealPath();
            if (!Files.isDirectory(canonicalPath)) {
                throw new InvalidRepositoryPathException("Path must be an existing directory");
            }
            if (!Files.isReadable(canonicalPath)) {
                throw new InvalidRepositoryPathException("Path must be readable");
            }
            return canonicalPath;
        } catch (IOException ex) {
            throw new InvalidRepositoryPathException("Path must be an existing readable directory", ex);
        }
    }
}

