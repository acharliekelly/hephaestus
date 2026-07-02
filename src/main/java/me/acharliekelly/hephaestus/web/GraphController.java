package me.acharliekelly.hephaestus.web;

import java.util.List;
import me.acharliekelly.hephaestus.graph.DependencyQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GraphController {
    private final DependencyQueryService dependencyQueryService;

    public GraphController(DependencyQueryService dependencyQueryService) {
        this.dependencyQueryService = dependencyQueryService;
    }

    @GetMapping("/repositories/{repositoryId}/symbols")
    public List<SymbolResponse> symbols(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "") String name
    ) {
        return dependencyQueryService.findSymbols(repositoryId, name).stream()
                .map(SymbolResponse::from)
                .toList();
    }

    @GetMapping("/symbols/{symbolId}/dependencies")
    public List<DependencyResponse> dependencies(@PathVariable Long symbolId) {
        return dependencyQueryService.dependenciesOf(symbolId).stream()
                .map(DependencyResponse::from)
                .toList();
    }

    @GetMapping("/symbols/{symbolId}/dependents")
    public List<DependencyResponse> dependents(@PathVariable Long symbolId) {
        return dependencyQueryService.dependentsOf(symbolId).stream()
                .map(DependencyResponse::from)
                .toList();
    }
}

