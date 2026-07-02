package me.acharliekelly.hephaestus.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import me.acharliekelly.hephaestus.graph.ArchitectureSummaryResponse;
import me.acharliekelly.hephaestus.graph.MermaidDiagramResponse;
import me.acharliekelly.hephaestus.indexing.IndexingService;
import me.acharliekelly.hephaestus.model.RepositoryRecord;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ArchitectureOutputApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepoService repoService;

    @Autowired
    private IndexingService indexingService;

    @Test
    void exposesArchitectureSummaryAndMermaidDiagramEndpoints(@TempDir Path projectRoot) throws Exception {
        writeLayeredFixture(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());
        indexingService.indexRepository(repository.getId());

        String summaryJson = mockMvc.perform(get("/api/repositories/{repositoryId}/architecture-summary", repository.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ArchitectureSummaryResponse summary = objectMapper.readValue(summaryJson, ArchitectureSummaryResponse.class);
        assertThat(summary.packageCount()).isEqualTo(3);

        String diagramJson = mockMvc.perform(get("/api/repositories/{repositoryId}/diagrams/dependencies", repository.getId())
                        .param("scope", "package"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        MermaidDiagramResponse diagram = objectMapper.readValue(diagramJson, MermaidDiagramResponse.class);
        assertThat(diagram.mermaid()).contains("com_example_web[com.example.web] --> com_example_service[com.example.service]");
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

