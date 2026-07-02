package me.acharliekelly.hephaestus.graph;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import me.acharliekelly.hephaestus.model.CodeSymbolRecord;
import me.acharliekelly.hephaestus.model.DependencyRecord;
import me.acharliekelly.hephaestus.model.SymbolKind;
import me.acharliekelly.hephaestus.model.persistence.CodeSymbolRecordRepository;
import me.acharliekelly.hephaestus.model.persistence.DependencyRecordRepository;
import me.acharliekelly.hephaestus.model.persistence.SourceFileRecordRepository;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArchitectureSummaryService {
    private final RepoService repoService;
    private final SourceFileRecordRepository sourceFiles;
    private final CodeSymbolRecordRepository symbols;
    private final DependencyRecordRepository dependencies;

    public ArchitectureSummaryService(
            RepoService repoService,
            SourceFileRecordRepository sourceFiles,
            CodeSymbolRecordRepository symbols,
            DependencyRecordRepository dependencies
    ) {
        this.repoService = repoService;
        this.sourceFiles = sourceFiles;
        this.symbols = symbols;
        this.dependencies = dependencies;
    }

    @Transactional(readOnly = true)
    public ArchitectureSummaryResponse summarize(Long repositoryId) {
        repoService.requireRepository(repositoryId);
        List<DependencyRecord> repositoryDependencies = dependencies.findByRepositoryId(repositoryId);
        List<CodeSymbolRecord> repositorySymbols = symbols.findByRepositoryId(repositoryId);

        return new ArchitectureSummaryResponse(
                repositoryId,
                sourceFiles.findByRepositoryId(repositoryId).size(),
                packageCount(repositorySymbols),
                symbolCounts(repositorySymbols),
                dependencyCounts(repositoryDependencies),
                packageDependencies(repositoryDependencies),
                classDependencies(repositoryDependencies),
                topDependedOnSymbols(repositoryDependencies)
        );
    }

    public List<PackageDependencyResponse> packageDependencies(List<DependencyRecord> repositoryDependencies) {
        return repositoryDependencies.stream()
                .filter(dependency -> dependency.getFromSymbol() != null && dependency.getTargetSymbol() != null)
                .filter(dependency -> dependency.getFromSymbol().getKind() == SymbolKind.CLASS)
                .filter(dependency -> dependency.getTargetSymbol().getKind() == SymbolKind.CLASS
                        || dependency.getTargetSymbol().getKind() == SymbolKind.INTERFACE
                        || dependency.getTargetSymbol().getKind() == SymbolKind.ENUM)
                .collect(Collectors.groupingBy(
                        dependency -> new PackageEdge(
                                dependency.getFromSymbol().getSourceFile().getPackageName(),
                                dependency.getTargetSymbol().getSourceFile().getPackageName()
                        ),
                        TreeMap::new,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().fromPackage().equals(entry.getKey().toPackage()))
                .map(entry -> new PackageDependencyResponse(
                        entry.getKey().fromPackage(),
                        entry.getKey().toPackage(),
                        entry.getValue()
                ))
                .toList();
    }

    public List<SymbolDependencySummaryResponse> classDependencies(List<DependencyRecord> repositoryDependencies) {
        return repositoryDependencies.stream()
                .filter(dependency -> dependency.getFromSymbol() != null && dependency.getTargetSymbol() != null)
                .filter(dependency -> dependency.getFromSymbol().getKind() == SymbolKind.CLASS)
                .filter(dependency -> dependency.getTargetSymbol().getKind() == SymbolKind.CLASS
                        || dependency.getTargetSymbol().getKind() == SymbolKind.INTERFACE
                        || dependency.getTargetSymbol().getKind() == SymbolKind.ENUM)
                .collect(Collectors.groupingBy(
                        dependency -> new SymbolEdge(
                                dependency.getFromSymbol().getQualifiedName(),
                                dependency.getTargetSymbol().getQualifiedName()
                        ),
                        TreeMap::new,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> new SymbolDependencySummaryResponse(
                        entry.getKey().fromQualifiedName(),
                        entry.getKey().toQualifiedName(),
                        entry.getValue()
                ))
                .toList();
    }

    private int packageCount(List<CodeSymbolRecord> repositorySymbols) {
        return (int) repositorySymbols.stream()
                .map(symbol -> symbol.getSourceFile().getPackageName())
                .distinct()
                .count();
    }

    private Map<String, Long> symbolCounts(List<CodeSymbolRecord> repositorySymbols) {
        return repositorySymbols.stream()
                .collect(Collectors.groupingBy(
                        symbol -> symbol.getKind().name(),
                        TreeMap::new,
                        Collectors.counting()
                ));
    }

    private Map<String, Long> dependencyCounts(List<DependencyRecord> repositoryDependencies) {
        return repositoryDependencies.stream()
                .collect(Collectors.groupingBy(
                        dependency -> dependency.getKind().name(),
                        TreeMap::new,
                        Collectors.counting()
                ));
    }

    private List<SymbolDependencySummaryResponse> topDependedOnSymbols(List<DependencyRecord> repositoryDependencies) {
        return repositoryDependencies.stream()
                .filter(dependency -> dependency.getFromSymbol() != null && dependency.getTargetSymbol() != null)
                .filter(dependency -> dependency.getFromSymbol().getKind() == SymbolKind.CLASS)
                .filter(dependency -> dependency.getTargetSymbol().getKind() == SymbolKind.CLASS
                        || dependency.getTargetSymbol().getKind() == SymbolKind.INTERFACE
                        || dependency.getTargetSymbol().getKind() == SymbolKind.ENUM)
                .collect(Collectors.groupingBy(
                        dependency -> dependency.getTargetSymbol().getQualifiedName(),
                        TreeMap::new,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new SymbolDependencySummaryResponse(null, entry.getKey(), entry.getValue()))
                .toList();
    }

    private record PackageEdge(String fromPackage, String toPackage) implements Comparable<PackageEdge> {
        @Override
        public int compareTo(PackageEdge other) {
            return Comparator.comparing(PackageEdge::fromPackage)
                    .thenComparing(PackageEdge::toPackage)
                    .compare(this, other);
        }
    }

    private record SymbolEdge(String fromQualifiedName, String toQualifiedName) implements Comparable<SymbolEdge> {
        @Override
        public int compareTo(SymbolEdge other) {
            return Comparator.comparing(SymbolEdge::fromQualifiedName)
                    .thenComparing(SymbolEdge::toQualifiedName)
                    .compare(this, other);
        }
    }
}
