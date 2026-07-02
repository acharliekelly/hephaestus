package me.acharliekelly.hephaestus.indexing;

import me.acharliekelly.hephaestus.model.DependencyKind;

public record ParsedDependency(
        String fromSymbolQualifiedName,
        String targetName,
        String targetQualifiedName,
        DependencyKind kind,
        Integer lineNumber
) {
}

