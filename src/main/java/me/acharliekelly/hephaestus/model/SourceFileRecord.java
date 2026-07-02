package me.acharliekelly.hephaestus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "source_files",
        uniqueConstraints = @UniqueConstraint(columnNames = {"repository_id", "relativePath"})
)
public class SourceFileRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryRecord repository;

    @Column(nullable = false, length = 2048)
    private String relativePath;

    @Column(nullable = false, length = 2048)
    private String absolutePath;

    @Column(nullable = false)
    private String packageName;

    protected SourceFileRecord() {
    }

    public SourceFileRecord(RepositoryRecord repository, String relativePath, String absolutePath, String packageName) {
        this.repository = repository;
        this.relativePath = relativePath;
        this.absolutePath = absolutePath;
        this.packageName = packageName;
    }

    public Long getId() {
        return id;
    }

    public RepositoryRecord getRepository() {
        return repository;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public String getPackageName() {
        return packageName;
    }
}

