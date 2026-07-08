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
        name = "endpoints",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"repository_id", "httpMethod", "path", "handler_symbol_id"}
        )
)
public class EndpointRecord {
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
    @JoinColumn(name = "controller_symbol_id")
    private CodeSymbolRecord controllerSymbol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_symbol_id")
    private CodeSymbolRecord handlerSymbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HttpMethod httpMethod;

    @Column(nullable = false, length = 2048)
    private String path;

    private Integer lineNumber;

    protected EndpointRecord() {
    }

    public EndpointRecord(
            RepositoryRecord repository,
            SourceFileRecord sourceFile,
            CodeSymbolRecord controllerSymbol,
            CodeSymbolRecord handlerSymbol,
            HttpMethod httpMethod,
            String path,
            Integer lineNumber
    ) {
        this.repository = repository;
        this.sourceFile = sourceFile;
        this.controllerSymbol = controllerSymbol;
        this.handlerSymbol = handlerSymbol;
        this.httpMethod = httpMethod;
        this.path = path;
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

    public CodeSymbolRecord getControllerSymbol() {
        return controllerSymbol;
    }

    public CodeSymbolRecord getHandlerSymbol() {
        return handlerSymbol;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }
}
