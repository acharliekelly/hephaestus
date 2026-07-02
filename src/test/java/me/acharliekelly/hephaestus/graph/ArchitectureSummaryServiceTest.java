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
class ArchitectureSummaryServiceTest {
    @Autowired
    private RepoService repoService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private ArchitectureSummaryService architectureSummaryService;

    @Test
    void summarizesRepositoryArchitectureFromPersistedFacts(@TempDir Path projectRoot) throws Exception {
        writeLayeredFixture(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());
        indexingService.indexRepository(repository.getId());

        ArchitectureSummaryResponse summary = architectureSummaryService.summarize(repository.getId());

        assertThat(summary.repositoryId()).isEqualTo(repository.getId());
        assertThat(summary.sourceFileCount()).isEqualTo(3);
        assertThat(summary.packageCount()).isEqualTo(3);
        assertThat(summary.symbolCounts()).containsEntry("CLASS", 3L);
        assertThat(summary.dependencyCounts()).containsEntry("FIELD_TYPE", 2L);
        assertThat(summary.packageDependencies())
                .extracting(PackageDependencyResponse::fromPackage, PackageDependencyResponse::toPackage, PackageDependencyResponse::dependencyCount)
                .containsExactly(
                        tuple("com.example.service", "com.example.repo", 1L),
                        tuple("com.example.web", "com.example.service", 1L)
                );
        assertThat(summary.classDependencies())
                .extracting(SymbolDependencySummaryResponse::fromQualifiedName, SymbolDependencySummaryResponse::toQualifiedName, SymbolDependencySummaryResponse::dependencyCount)
                .containsExactly(
                        tuple("com.example.service.PaymentService", "com.example.repo.PaymentRepository", 1L),
                        tuple("com.example.web.PaymentController", "com.example.service.PaymentService", 1L)
                );
        assertThat(summary.topDependedOnSymbols())
                .extracting(SymbolDependencySummaryResponse::toQualifiedName, SymbolDependencySummaryResponse::dependencyCount)
                .containsExactly(
                        tuple("com.example.repo.PaymentRepository", 1L),
                        tuple("com.example.service.PaymentService", 1L)
                );
    }

    private void writeLayeredFixture(Path projectRoot) throws Exception {
        Files.createDirectories(projectRoot.resolve("src/main/java/com/example/web"));
        Files.createDirectories(projectRoot.resolve("src/main/java/com/example/service"));
        Files.createDirectories(projectRoot.resolve("src/main/java/com/example/repo"));
        Files.writeString(projectRoot.resolve("src/main/java/com/example/web/PaymentController.java"), """
                package com.example.web;

                import com.example.service.PaymentService;

                public class PaymentController {
                    private PaymentService paymentService;
                }
                """);
        Files.writeString(projectRoot.resolve("src/main/java/com/example/service/PaymentService.java"), """
                package com.example.service;

                import com.example.repo.PaymentRepository;

                public class PaymentService {
                    private PaymentRepository paymentRepository;
                }
                """);
        Files.writeString(projectRoot.resolve("src/main/java/com/example/repo/PaymentRepository.java"), """
                package com.example.repo;

                public class PaymentRepository {
                }
                """);
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.api.Assertions.tuple(values);
    }
}
