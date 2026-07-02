package me.acharliekelly.hephaestus.web;

import jakarta.validation.Valid;
import me.acharliekelly.hephaestus.indexing.IndexingResult;
import me.acharliekelly.hephaestus.indexing.IndexingService;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
public class ImportController {
    private final RepoService repoService;
    private final IndexingService indexingService;

    public ImportController(RepoService repoService, IndexingService indexingService) {
        this.repoService = repoService;
        this.indexingService = indexingService;
    }

    @PostMapping("/import-local")
    @ResponseStatus(HttpStatus.CREATED)
    public RepositoryResponse importLocal(@Valid @RequestBody ImportLocalRepositoryRequest request) {
        return RepositoryResponse.from(repoService.importLocal(request.path()));
    }

    @PostMapping("/{repositoryId}/index")
    public IndexingResult index(@PathVariable Long repositoryId) {
        return indexingService.indexRepository(repositoryId);
    }
}

