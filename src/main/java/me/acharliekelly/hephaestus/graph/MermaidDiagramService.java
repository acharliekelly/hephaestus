package me.acharliekelly.hephaestus.graph;

import java.util.List;
import me.acharliekelly.hephaestus.model.persistence.DependencyRecordRepository;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MermaidDiagramService {
    private final RepoService repoService;
    private final DependencyRecordRepository dependencies;
    private final ArchitectureSummaryService architectureSummaryService;

    public MermaidDiagramService(
            RepoService repoService,
            DependencyRecordRepository dependencies,
            ArchitectureSummaryService architectureSummaryService
    ) {
        this.repoService = repoService;
        this.dependencies = dependencies;
        this.architectureSummaryService = architectureSummaryService;
    }

    @Transactional(readOnly = true)
    public MermaidDiagramResponse dependencyDiagram(Long repositoryId, DiagramScope scope) {
        repoService.requireRepository(repositoryId);
        String mermaid = switch (scope) {
            case PACKAGE -> packageDiagram(repositoryId);
            case CLASS -> classDiagram(repositoryId);
        };
        return new MermaidDiagramResponse(repositoryId, scope, mermaid);
    }

    private String packageDiagram(Long repositoryId) {
        List<PackageDependencyResponse> packageDependencies = architectureSummaryService.packageDependencies(
                dependencies.findByRepositoryId(repositoryId)
        );
        return render(packageDependencies.stream()
                .map(dependency -> "  " + nodeId(dependency.fromPackage())
                        + "[" + dependency.fromPackage() + "] --> "
                        + nodeId(dependency.toPackage())
                        + "[" + dependency.toPackage() + "]")
                .toList());
    }

    private String classDiagram(Long repositoryId) {
        List<SymbolDependencySummaryResponse> classDependencies = architectureSummaryService.classDependencies(
                dependencies.findByRepositoryId(repositoryId)
        );
        return render(classDependencies.stream()
                .map(dependency -> "  " + nodeId(dependency.fromQualifiedName())
                        + "[" + dependency.fromQualifiedName() + "] --> "
                        + nodeId(dependency.toQualifiedName())
                        + "[" + dependency.toQualifiedName() + "]")
                .toList());
    }

    private String render(List<String> edges) {
        if (edges.isEmpty()) {
            return "graph TD\n";
        }
        return "graph TD\n" + String.join("\n", edges) + "\n";
    }

    private String nodeId(String label) {
        return label.replaceAll("[^A-Za-z0-9]", "_");
    }
}

