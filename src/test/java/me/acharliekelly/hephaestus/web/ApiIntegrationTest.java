package me.acharliekelly.hephaestus.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import me.acharliekelly.hephaestus.indexing.IndexingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void importsIndexesAndQueriesDependencyEndpoints(@TempDir Path projectRoot) throws Exception {
        writeFixture(projectRoot);

        String importJson = mockMvc.perform(post("/api/repositories/import-local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ImportLocalRepositoryRequest(projectRoot.toString()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long repositoryId = objectMapper.readValue(importJson, RepositoryResponse.class).id();

        String indexJson = mockMvc.perform(post("/api/repositories/{repositoryId}/index", repositoryId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readValue(indexJson, IndexingResult.class).symbolCount()).isGreaterThanOrEqualTo(3);

        String symbolsJson = mockMvc.perform(get("/api/repositories/{repositoryId}/symbols", repositoryId)
                        .param("name", "PaymentService"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<SymbolResponse> symbols = objectMapper.readValue(symbolsJson, new TypeReference<>() {
        });
        SymbolResponse paymentService = symbols.getFirst();
        assertThat(paymentService.qualifiedName()).isEqualTo("com.example.PaymentService");
        assertThat(paymentService.sourcePath()).endsWith("PaymentService.java");

        String dependenciesJson = mockMvc.perform(get("/api/symbols/{symbolId}/dependencies", paymentService.id()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<DependencyResponse> dependencies = objectMapper.readValue(dependenciesJson, new TypeReference<>() {
        });
        assertThat(dependencies)
                .extracting(DependencyResponse::targetName)
                .contains("PaymentPort");

        String portJson = mockMvc.perform(get("/api/repositories/{repositoryId}/symbols", repositoryId)
                        .param("name", "PaymentPort"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<SymbolResponse> ports = objectMapper.readValue(portJson, new TypeReference<>() {
        });
        Long portId = ports.getFirst().id();
        String dependentsJson = mockMvc.perform(get("/api/symbols/{symbolId}/dependents", portId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<DependencyResponse> dependents = objectMapper.readValue(dependentsJson, new TypeReference<>() {
        });
        assertThat(dependents)
                .extracting(DependencyResponse::targetName)
                .contains("PaymentPort");
    }

    @Test
    void indexingPersistenceFailuresReturnUnprocessableEntity(@TempDir Path projectRoot) throws Exception {
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

        String importJson = mockMvc.perform(post("/api/repositories/import-local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ImportLocalRepositoryRequest(projectRoot.toString()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long repositoryId = objectMapper.readValue(importJson, RepositoryResponse.class).id();

        String errorJson = mockMvc.perform(post("/api/repositories/{repositoryId}/index", repositoryId))
                .andExpect(status().isUnprocessableEntity())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readValue(errorJson, ErrorResponse.class).message())
                .contains("Failed to persist architecture facts");
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
}
