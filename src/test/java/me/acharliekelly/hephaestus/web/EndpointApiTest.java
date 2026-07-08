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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EndpointApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsIndexedEndpoints(@TempDir Path projectRoot) throws Exception {
        writeController(projectRoot);

        String importJson = mockMvc.perform(post("/api/repositories/import-local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ImportLocalRepositoryRequest(projectRoot.toString()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long repositoryId = objectMapper.readValue(importJson, RepositoryResponse.class).id();

        mockMvc.perform(post("/api/repositories/{repositoryId}/index", repositoryId))
                .andExpect(status().isOk());

        String endpointsJson = mockMvc.perform(get("/api/repositories/{repositoryId}/endpoints", repositoryId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<EndpointResponse> endpoints = objectMapper.readValue(endpointsJson, new TypeReference<>() {
        });

        assertThat(endpoints)
                .extracting(EndpointResponse::httpMethod, EndpointResponse::path)
                .contains(org.assertj.core.api.Assertions.tuple("GET", "/api/files"));
    }

    private void writeController(Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/web"));
        Files.writeString(packageDir.resolve("FileController.java"), """
                package com.example.web;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/api/files")
                public class FileController {
                    @GetMapping
                    public String list() {
                        return "ok";
                    }
                }
                """);
    }
}
