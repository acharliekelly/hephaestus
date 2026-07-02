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
        name = "dependencies",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"repository_id", "from_symbol_id", "targetName", "kind", "lineNumber"}
        )
)
public class DependencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryRecord repository;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_file_id", nullable = false)
    private SourceFileRecord sourceFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_symbol_id")
    private CodeSymbolRecord fromSymbol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_symbol_id")
    private CodeSymbolRecord targetSymbol;

    @Column(nullable = false)
    private String targetName;

    @Column(length = 2048)
    private String targetQualifiedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DependencyKind kind;

    private Integer lineNumber;

    protected DependencyRecord() {
    }

    public DependencyRecord(
            RepositoryRecord repository,
            SourceFileRecord sourceFile,
            CodeSymbolRecord fromSymbol,
            CodeSymbolRecord targetSymbol,
            String targetName,
            String targetQualifiedName,
            DependencyKind kind,
            Integer lineNumber
    ) {
        this.repository = repository;
        this.sourceFile = sourceFile;
        this.fromSymbol = fromSymbol;
        this.targetSymbol = targetSymbol;
        this.targetName = targetName;
        this.targetQualifiedName = targetQualifiedName;
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

    public CodeSymbolRecord getFromSymbol() {
        return fromSymbol;
    }

    public CodeSymbolRecord getTargetSymbol() {
        return targetSymbol;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetQualifiedName() {
        return targetQualifiedName;
    }

    public DependencyKind getKind() {
        return kind;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }
}

