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
class MermaidDiagramServiceTest {
    @Autowired
    private RepoService repoService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private MermaidDiagramService mermaidDiagramService;

    @Test
    void createsStablePackageDependencyDiagram(@TempDir Path projectRoot) throws Exception {
        writeLayeredFixture(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());
        indexingService.indexRepository(repository.getId());

        MermaidDiagramResponse diagram = mermaidDiagramService.dependencyDiagram(repository.getId(), DiagramScope.PACKAGE);

        assertThat(diagram.scope()).isEqualTo(DiagramScope.PACKAGE);
        assertThat(diagram.mermaid()).isEqualTo("""
                graph TD
                  com_example_service[com.example.service] --> com_example_repo[com.example.repo]
                  com_example_web[com.example.web] --> com_example_service[com.example.service]
                """);
    }

    @Test
    void createsStableClassDependencyDiagram(@TempDir Path projectRoot) throws Exception {
        writeLayeredFixture(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());
        indexingService.indexRepository(repository.getId());

        MermaidDiagramResponse diagram = mermaidDiagramService.dependencyDiagram(repository.getId(), DiagramScope.CLASS);

        assertThat(diagram.scope()).isEqualTo(DiagramScope.CLASS);
        assertThat(diagram.mermaid()).isEqualTo("""
                graph TD
                  com_example_service_PaymentService[com.example.service.PaymentService] --> com_example_repo_PaymentRepository[com.example.repo.PaymentRepository]
                  com_example_web_PaymentController[com.example.web.PaymentController] --> com_example_service_PaymentService[com.example.service.PaymentService]
                """);
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
}

