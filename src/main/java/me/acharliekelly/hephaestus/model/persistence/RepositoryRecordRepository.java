package me.acharliekelly.hephaestus.model.persistence;

import java.util.Optional;
import me.acharliekelly.hephaestus.model.RepositoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryRecordRepository extends JpaRepository<RepositoryRecord, Long> {
    Optional<RepositoryRecord> findByCanonicalPath(String canonicalPath);
}

