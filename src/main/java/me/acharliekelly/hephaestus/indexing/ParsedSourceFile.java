package me.acharliekelly.hephaestus.indexing;

import java.util.List;

public record ParsedSourceFile(
        String relativePath,
        String absolutePath,
        String packageName,
        List<ParsedSymbol> symbols,
        List<ParsedDependency> dependencies,
        List<ParsedEndpoint> endpoints
) {
}
