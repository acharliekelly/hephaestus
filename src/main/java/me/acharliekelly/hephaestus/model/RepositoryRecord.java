package me.acharliekelly.hephaestus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "repositories")
public class RepositoryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 2048)
    private String canonicalPath;

    @Column(nullable = false)
    private Instant importedAt;

    protected RepositoryRecord() {
    }

    public RepositoryRecord(String name, String canonicalPath, Instant importedAt) {
        this.name = name;
        this.canonicalPath = canonicalPath;
        this.importedAt = importedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCanonicalPath() {
        return canonicalPath;
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}

