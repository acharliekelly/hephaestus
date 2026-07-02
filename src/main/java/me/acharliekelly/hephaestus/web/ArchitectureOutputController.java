package me.acharliekelly.hephaestus.web;

import me.acharliekelly.hephaestus.graph.ArchitectureSummaryResponse;
import me.acharliekelly.hephaestus.graph.ArchitectureSummaryService;
import me.acharliekelly.hephaestus.graph.DiagramScope;
import me.acharliekelly.hephaestus.graph.MermaidDiagramResponse;
import me.acharliekelly.hephaestus.graph.MermaidDiagramService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories/{repositoryId}")
public class ArchitectureOutputController {
    private final ArchitectureSummaryService architectureSummaryService;
    private final MermaidDiagramService mermaidDiagramService;

    public ArchitectureOutputController(
            ArchitectureSummaryService architectureSummaryService,
            MermaidDiagramService mermaidDiagramService
    ) {
        this.architectureSummaryService = architectureSummaryService;
        this.mermaidDiagramService = mermaidDiagramService;
    }

    @GetMapping("/architecture-summary")
    public ArchitectureSummaryResponse architectureSummary(@PathVariable Long repositoryId) {
        return architectureSummaryService.summarize(repositoryId);
    }

    @GetMapping("/diagrams/dependencies")
    public MermaidDiagramResponse dependencyDiagram(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "package") String scope
    ) {
        return mermaidDiagramService.dependencyDiagram(repositoryId, DiagramScope.valueOf(scope.toUpperCase()));
    }
}
