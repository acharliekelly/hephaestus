package me.acharliekelly.hephaestus.web;

import me.acharliekelly.hephaestus.model.CodeSymbolRecord;
import me.acharliekelly.hephaestus.model.SymbolKind;

public record SymbolResponse(
        Long id,
        String name,
        String qualifiedName,
        SymbolKind kind,
        String sourcePath,
        Integer lineNumber
) {
    public static SymbolResponse from(CodeSymbolRecord symbol) {
        return new SymbolResponse(
                symbol.getId(),
                symbol.getName(),
                symbol.getQualifiedName(),
                symbol.getKind(),
                symbol.getSourceFile().getRelativePath(),
                symbol.getLineNumber()
        );
    }
}

