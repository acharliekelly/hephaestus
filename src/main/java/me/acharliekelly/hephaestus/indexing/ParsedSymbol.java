package me.acharliekelly.hephaestus.indexing;

import me.acharliekelly.hephaestus.model.SymbolKind;

public record ParsedSymbol(
        String name,
        String qualifiedName,
        SymbolKind kind,
        Integer lineNumber
) {
}

