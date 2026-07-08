package me.acharliekelly.hephaestus.graph;

import java.util.Comparator;
import java.util.List;
import me.acharliekelly.hephaestus.model.EndpointRecord;
import me.acharliekelly.hephaestus.model.persistence.EndpointRecordRepository;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EndpointQueryService {
    private final RepoService repoService;
    private final EndpointRecordRepository endpoints;

    public EndpointQueryService(RepoService repoService, EndpointRecordRepository endpoints) {
        this.repoService = repoService;
        this.endpoints = endpoints;
    }

    @Transactional(readOnly = true)
    public List<EndpointRecord> findEndpoints(Long repositoryId, String path) {
        repoService.requireRepository(repositoryId);
        String pathFilter = path == null ? "" : path;
        return endpoints.findByRepositoryId(repositoryId).stream()
                .filter(endpoint -> pathFilter.isBlank() || endpoint.getPath().contains(pathFilter))
                .sorted(Comparator.comparing(EndpointRecord::getPath)
                        .thenComparing(endpoint -> endpoint.getHttpMethod().name()))
                .toList();
    }
}
