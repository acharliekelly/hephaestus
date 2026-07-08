package me.acharliekelly.hephaestus.web;

import me.acharliekelly.hephaestus.model.EndpointRecord;

public record EndpointResponse(
        Long id,
        String httpMethod,
        String path,
        Long controllerSymbolId,
        Long handlerSymbolId,
        String sourcePath,
        Integer lineNumber
) {
    public static EndpointResponse from(EndpointRecord endpoint) {
        return new EndpointResponse(
                endpoint.getId(),
                endpoint.getHttpMethod().name(),
                endpoint.getPath(),
                endpoint.getControllerSymbol() == null ? null : endpoint.getControllerSymbol().getId(),
                endpoint.getHandlerSymbol() == null ? null : endpoint.getHandlerSymbol().getId(),
                endpoint.getSourceFile().getRelativePath(),
                endpoint.getLineNumber()
        );
    }
}
