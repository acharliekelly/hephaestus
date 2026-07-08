package me.acharliekelly.hephaestus.indexing;

public record ParsedEndpoint(
        String httpMethod,
        String path,
        String controllerQualifiedName,
        String handlerQualifiedName,
        Integer lineNumber
) {
}
