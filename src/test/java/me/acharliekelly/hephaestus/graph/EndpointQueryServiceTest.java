package me.acharliekelly.hephaestus.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import me.acharliekelly.hephaestus.indexing.IndexingService;
import me.acharliekelly.hephaestus.model.RepositoryRecord;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EndpointQueryServiceTest {
    @Autowired
    private RepoService repoService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private EndpointQueryService endpointQueryService;

    @Test
    void findsEndpointsWithOptionalPathFilter(@TempDir Path projectRoot) throws Exception {
        writeController(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());
        indexingService.indexRepository(repository.getId());

        assertThat(endpointQueryService.findEndpoints(repository.getId(), ""))
                .extracting(endpoint -> endpoint.getHttpMethod().name(), endpoint -> endpoint.getPath())
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("GET", "/api/files"),
                        org.assertj.core.api.Assertions.tuple("POST", "/api/files")
                );

        assertThat(endpointQueryService.findEndpoints(repository.getId(), "/api/files"))
                .hasSize(2);
    }

    private void writeController(Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/web"));
        Files.writeString(packageDir.resolve("FileController.java"), """
                package com.example.web;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/api/files")
                public class FileController {
                    @GetMapping
                    public String list() { return "ok"; }

                    @PostMapping
                    public String create() { return "ok"; }
                }
                """);
    }
}
