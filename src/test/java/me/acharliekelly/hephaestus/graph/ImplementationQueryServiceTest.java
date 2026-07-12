package me.acharliekelly.hephaestus.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import me.acharliekelly.hephaestus.indexing.IndexingService;
import me.acharliekelly.hephaestus.model.CodeSymbolRecord;
import me.acharliekelly.hephaestus.model.RepositoryRecord;
import me.acharliekelly.hephaestus.model.persistence.CodeSymbolRecordRepository;
import me.acharliekelly.hephaestus.repo.NotFoundException;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ImplementationQueryServiceTest {
    @Autowired
    private RepoService repoService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private DependencyQueryService dependencyQueryService;

    @Autowired
    private CodeSymbolRecordRepository symbols;

    @Test
    void findsClassesThatImplementInterface(@TempDir Path projectRoot) throws Exception {
        writeInterfaceFixture(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());
        indexingService.indexRepository(repository.getId());

        CodeSymbolRecord paymentPort = onlySymbol(repository.getId(), "PaymentPort");

        assertThat(dependencyQueryService.implementationsOf(paymentPort.getId()))
                .extracting(CodeSymbolRecord::getQualifiedName)
                .containsExactly(
                        "com.example.CardPaymentService",
                        "com.example.WirePaymentService"
                );
    }

    @Test
    void returnsEmptyListForConcreteClassSymbol(@TempDir Path projectRoot) throws Exception {
        writeInterfaceFixture(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());
        indexingService.indexRepository(repository.getId());

        CodeSymbolRecord cardPaymentService = onlySymbol(repository.getId(), "CardPaymentService");

        assertThat(dependencyQueryService.implementationsOf(cardPaymentService.getId()))
                .isEmpty();
    }

    @Test
    void rejectsMissingSymbol() {
        assertThatThrownBy(() -> dependencyQueryService.implementationsOf(999_999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Symbol 999999 was not found");
    }

    private CodeSymbolRecord onlySymbol(Long repositoryId, String name) {
        List<CodeSymbolRecord> matches = symbols.findByRepositoryIdAndNameContainingIgnoreCase(repositoryId, name);
        assertThat(matches).hasSize(1);
        return matches.getFirst();
    }

    private void writeInterfaceFixture(Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example"));
        Files.writeString(packageDir.resolve("PaymentPort.java"), """
                package com.example;

                public interface PaymentPort {
                    void charge();
                }
                """);
        Files.writeString(packageDir.resolve("CardPaymentService.java"), """
                package com.example;

                public class CardPaymentService implements PaymentPort {
                    public void charge() {
                    }
                }
                """);
        Files.writeString(packageDir.resolve("WirePaymentService.java"), """
                package com.example;

                public class WirePaymentService implements PaymentPort {
                    public void charge() {
                    }
                }
                """);
    }
}
