package me.acharliekelly.hephaestus.web;

import java.util.List;
import me.acharliekelly.hephaestus.graph.EndpointQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories/{repositoryId}/endpoints")
public class EndpointController {
    private final EndpointQueryService endpointQueryService;

    public EndpointController(EndpointQueryService endpointQueryService) {
        this.endpointQueryService = endpointQueryService;
    }

    @GetMapping
    public List<EndpointResponse> endpoints(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "") String path
    ) {
        return endpointQueryService.findEndpoints(repositoryId, path).stream()
                .map(EndpointResponse::from)
                .toList();
    }
}
