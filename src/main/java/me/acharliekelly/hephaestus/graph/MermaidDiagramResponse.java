package me.acharliekelly.hephaestus.graph;

public record MermaidDiagramResponse(
        Long repositoryId,
        DiagramScope scope,
        String mermaid
) {
}

