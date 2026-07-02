package me.acharliekelly.hephaestus.web;

import me.acharliekelly.hephaestus.model.DependencyKind;
import me.acharliekelly.hephaestus.model.DependencyRecord;

public record DependencyResponse(
        Long id,
        Long fromSymbolId,
        Long targetSymbolId,
        String targetName,
        String targetQualifiedName,
        DependencyKind kind,
        String sourcePath,
        Integer lineNumber
) {
    public static DependencyResponse from(DependencyRecord dependency) {
        Long fromSymbolId = dependency.getFromSymbol() == null ? null : dependency.getFromSymbol().getId();
        Long targetSymbolId = dependency.getTargetSymbol() == null ? null : dependency.getTargetSymbol().getId();
        return new DependencyResponse(
                dependency.getId(),
                fromSymbolId,
                targetSymbolId,
                dependency.getTargetName(),
                dependency.getTargetQualifiedName(),
                dependency.getKind(),
                dependency.getSourceFile().getRelativePath(),
                dependency.getLineNumber()
        );
    }
}

