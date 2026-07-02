package me.acharliekelly.hephaestus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "code_symbols",
        uniqueConstraints = @UniqueConstraint(columnNames = {"repository_id", "qualifiedName", "kind"})
)
public class CodeSymbolRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryRecord repository;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_file_id", nullable = false)
    private SourceFileRecord sourceFile;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2048)
    private String qualifiedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SymbolKind kind;

    private Integer lineNumber;

    protected CodeSymbolRecord() {
    }

    public CodeSymbolRecord(
            RepositoryRecord repository,
            SourceFileRecord sourceFile,
            String name,
            String qualifiedName,
            SymbolKind kind,
            Integer lineNumber
    ) {
        this.repository = repository;
        this.sourceFile = sourceFile;
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.kind = kind;
        this.lineNumber = lineNumber;
    }

    public Long getId() {
        return id;
    }

    public RepositoryRecord getRepository() {
        return repository;
    }

    public SourceFileRecord getSourceFile() {
        return sourceFile;
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }
}

