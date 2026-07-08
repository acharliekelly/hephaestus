package me.acharliekelly.hephaestus.indexing;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import me.acharliekelly.hephaestus.model.CodeSymbolRecord;
import me.acharliekelly.hephaestus.model.DependencyRecord;
import me.acharliekelly.hephaestus.model.EndpointRecord;
import me.acharliekelly.hephaestus.model.HttpMethod;
import me.acharliekelly.hephaestus.model.RepositoryRecord;
import me.acharliekelly.hephaestus.model.SourceFileRecord;
import me.acharliekelly.hephaestus.model.persistence.CodeSymbolRecordRepository;
import me.acharliekelly.hephaestus.model.persistence.DependencyRecordRepository;
import me.acharliekelly.hephaestus.model.persistence.EndpointRecordRepository;
import me.acharliekelly.hephaestus.model.persistence.SourceFileRecordRepository;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndexingService {
    private final RepoService repoService;
    private final JavaParserService javaParserService;
    private final SourceFileRecordRepository sourceFiles;
    private final CodeSymbolRecordRepository symbols;
    private final DependencyRecordRepository dependencies;
    private final EndpointRecordRepository endpoints;

    public IndexingService(
            RepoService repoService,
            JavaParserService javaParserService,
            SourceFileRecordRepository sourceFiles,
            CodeSymbolRecordRepository symbols,
            DependencyRecordRepository dependencies,
            EndpointRecordRepository endpoints
    ) {
        this.repoService = repoService;
        this.javaParserService = javaParserService;
        this.sourceFiles = sourceFiles;
        this.symbols = symbols;
        this.dependencies = dependencies;
        this.endpoints = endpoints;
    }

    @Transactional
    public IndexingResult indexRepository(Long repositoryId) {
        RepositoryRecord repository = repoService.requireRepository(repositoryId);
        endpoints.deleteByRepositoryId(repositoryId);
        endpoints.flush();
        dependencies.deleteByRepositoryId(repositoryId);
        dependencies.flush();
        symbols.deleteByRepositoryId(repositoryId);
        symbols.flush();
        sourceFiles.deleteByRepositoryId(repositoryId);
        sourceFiles.flush();

        ParsedProject parsedProject = javaParserService.parse(Path.of(repository.getCanonicalPath()));
        Map<String, CodeSymbolRecord> savedSymbols = new HashMap<>();
        Map<ParsedSourceFile, SourceFileRecord> savedSourceFiles = new HashMap<>();

        try {
            for (ParsedSourceFile parsedSourceFile : parsedProject.sourceFiles()) {
                SourceFileRecord sourceFile = sourceFiles.save(new SourceFileRecord(
                        repository,
                        parsedSourceFile.relativePath(),
                        parsedSourceFile.absolutePath(),
                        parsedSourceFile.packageName()
                ));
                savedSourceFiles.put(parsedSourceFile, sourceFile);
                for (ParsedSymbol parsedSymbol : parsedSourceFile.symbols()) {
                    CodeSymbolRecord symbol = symbols.save(new CodeSymbolRecord(
                            repository,
                            sourceFile,
                            parsedSymbol.name(),
                            parsedSymbol.qualifiedName(),
                            parsedSymbol.kind(),
                            parsedSymbol.lineNumber()
                    ));
                    savedSymbols.put(symbol.getQualifiedName(), symbol);
                }
            }

            Set<String> dependencyKeys = new LinkedHashSet<>();
            int dependencyCount = 0;
            for (ParsedSourceFile parsedSourceFile : parsedProject.sourceFiles()) {
                SourceFileRecord sourceFile = savedSourceFiles.get(parsedSourceFile);
                for (ParsedDependency parsedDependency : parsedSourceFile.dependencies()) {
                    CodeSymbolRecord fromSymbol = savedSymbols.get(parsedDependency.fromSymbolQualifiedName());
                    CodeSymbolRecord targetSymbol = resolveTargetSymbol(savedSymbols, parsedDependency);
                    String key = dependencyKey(sourceFile, fromSymbol, targetSymbol, parsedDependency);
                    if (dependencyKeys.add(key)) {
                        dependencies.save(new DependencyRecord(
                                repository,
                                sourceFile,
                                fromSymbol,
                                targetSymbol,
                                parsedDependency.targetName(),
                                parsedDependency.targetQualifiedName(),
                                parsedDependency.kind(),
                                parsedDependency.lineNumber()
                        ));
                        dependencyCount++;
                    }
                }
            }

            for (ParsedSourceFile parsedSourceFile : parsedProject.sourceFiles()) {
                SourceFileRecord sourceFile = savedSourceFiles.get(parsedSourceFile);
                for (ParsedEndpoint parsedEndpoint : parsedSourceFile.endpoints()) {
                    endpoints.save(new EndpointRecord(
                            repository,
                            sourceFile,
                            savedSymbols.get(parsedEndpoint.controllerQualifiedName()),
                            savedSymbols.get(parsedEndpoint.handlerQualifiedName()),
                            HttpMethod.valueOf(parsedEndpoint.httpMethod()),
                            parsedEndpoint.path(),
                            parsedEndpoint.lineNumber()
                    ));
                }
            }

            return new IndexingResult(
                    repository.getId(),
                    savedSourceFiles.size(),
                    savedSymbols.size(),
                    dependencyCount
            );
        } catch (DataAccessException ex) {
            throw new IndexingException("Failed to persist architecture facts for repository " + repositoryId, ex);
        }
    }

    private CodeSymbolRecord resolveTargetSymbol(
            Map<String, CodeSymbolRecord> savedSymbols,
            ParsedDependency parsedDependency
    ) {
        if (parsedDependency.targetQualifiedName() != null) {
            CodeSymbolRecord exact = savedSymbols.get(parsedDependency.targetQualifiedName());
            if (exact != null) {
                return exact;
            }
        }
        return savedSymbols.values().stream()
                .filter(symbol -> symbol.getName().equals(parsedDependency.targetName()))
                .findFirst()
                .orElse(null);
    }

    private String dependencyKey(
            SourceFileRecord sourceFile,
            CodeSymbolRecord fromSymbol,
            CodeSymbolRecord targetSymbol,
            ParsedDependency parsedDependency
    ) {
        Long fromId = fromSymbol == null ? null : fromSymbol.getId();
        Long targetId = targetSymbol == null ? null : targetSymbol.getId();
        return sourceFile.getRelativePath()
                + "|" + fromId
                + "|" + targetId
                + "|" + parsedDependency.targetName()
                + "|" + parsedDependency.kind()
                + "|" + parsedDependency.lineNumber();
    }
}
