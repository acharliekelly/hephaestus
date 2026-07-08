package me.acharliekelly.hephaestus.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import me.acharliekelly.hephaestus.model.EndpointRecord;
import me.acharliekelly.hephaestus.model.RepositoryRecord;
import me.acharliekelly.hephaestus.model.persistence.CodeSymbolRecordRepository;
import me.acharliekelly.hephaestus.model.persistence.DependencyRecordRepository;
import me.acharliekelly.hephaestus.model.persistence.EndpointRecordRepository;
import me.acharliekelly.hephaestus.model.persistence.SourceFileRecordRepository;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IndexingServiceTest {
    @Autowired
    private RepoService repoService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private SourceFileRecordRepository sourceFiles;

    @Autowired
    private CodeSymbolRecordRepository symbols;

    @Autowired
    private DependencyRecordRepository dependencies;

    @Autowired
    private EndpointRecordRepository endpoints;

    @Test
    void persistsFactsAndReindexingDoesNotDuplicateThem(@TempDir Path projectRoot) throws Exception {
        writeFixture(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());

        IndexingResult first = indexingService.indexRepository(repository.getId());
        IndexingResult second = indexingService.indexRepository(repository.getId());

        assertThat(first.sourceFileCount()).isEqualTo(2);
        assertThat(first.symbolCount()).isGreaterThanOrEqualTo(4);
        assertThat(first.dependencyCount()).isGreaterThanOrEqualTo(3);
        assertThat(second).isEqualTo(first);
        assertThat(sourceFiles.findByRepositoryId(repository.getId())).hasSize(first.sourceFileCount());
        assertThat(symbols.findByRepositoryId(repository.getId())).hasSize(first.symbolCount());
        assertThat(dependencies.findByRepositoryId(repository.getId())).hasSize(first.dependencyCount());
    }

    @Test
    void wrapsPersistenceFailuresAsIndexingExceptions(@TempDir Path projectRoot) throws Exception {
        Path firstPackageDir = Files.createDirectories(projectRoot.resolve("src/main/java/first/com/example"));
        Path secondPackageDir = Files.createDirectories(projectRoot.resolve("src/main/java/second/com/example"));
        Files.writeString(firstPackageDir.resolve("DuplicateService.java"), """
                package com.example;

                public class DuplicateService {
                }
                """);
        Files.writeString(secondPackageDir.resolve("DuplicateService.java"), """
                package com.example;

                public class DuplicateService {
                }
                """);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());

        assertThatThrownBy(() -> indexingService.indexRepository(repository.getId()))
                .isInstanceOf(IndexingException.class)
                .hasMessageContaining("Failed to persist architecture facts");
    }

    @Test
    void persistsEndpointFactsAndReindexingDoesNotDuplicateThem(@TempDir Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/web"));
        Files.writeString(packageDir.resolve("FileController.java"), """
                package com.example.web;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/api/files")
                public class FileController {
                    @GetMapping("/{id}")
                    public String getFile(@PathVariable String id) {
                        return id;
                    }
                }
                """);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());

        indexingService.indexRepository(repository.getId());
        indexingService.indexRepository(repository.getId());

        assertThat(endpoints.findByRepositoryId(repository.getId()))
                .hasSize(1)
                .extracting(endpoint -> endpoint.getHttpMethod().name(), EndpointRecord::getPath)
                .containsExactly(tuple("GET", "/api/files/{id}"));
    }

    private void writeFixture(Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example"));
        Files.writeString(packageDir.resolve("PaymentPort.java"), """
                package com.example;

                public interface PaymentPort {
                    void charge();
                }
                """);
        Files.writeString(packageDir.resolve("PaymentService.java"), """
                package com.example;

                public class PaymentService implements PaymentPort {
                    private PaymentPort port;

                    public void charge() {
                        port.charge();
                    }
                }
                """);
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.api.Assertions.tuple(values);
    }
}
