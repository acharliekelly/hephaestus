package me.acharliekelly.hephaestus.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import me.acharliekelly.hephaestus.model.DependencyKind;
import me.acharliekelly.hephaestus.model.SymbolKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaParserServiceTest {
    private final JavaParserService javaParserService = new JavaParserService();

    @Test
    void parsesSymbolsAndDeterministicDependencies(@TempDir Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/payments"));
        Files.writeString(packageDir.resolve("PaymentPort.java"), """
                package com.example.payments;

                public interface PaymentPort {
                    void charge();
                }
                """);
        Files.writeString(packageDir.resolve("PaymentService.java"), """
                package com.example.payments;

                import java.util.List;

                @Deprecated
                public class PaymentService extends BaseService implements PaymentPort {
                    private List<String> attempts;

                    @Override
                    public void charge() {
                        recordAttempt();
                    }

                    private void recordAttempt() {
                    }
                }

                enum PaymentState {
                    OPEN
                }
                """);

        ParsedProject parsedProject = javaParserService.parse(projectRoot);

        assertThat(parsedProject.sourceFiles()).hasSize(2);
        assertThat(parsedProject.sourceFiles())
                .flatExtracting(ParsedSourceFile::symbols)
                .extracting(ParsedSymbol::qualifiedName, ParsedSymbol::kind)
                .contains(
                        tuple("com.example.payments.PaymentPort", SymbolKind.INTERFACE),
                        tuple("com.example.payments.PaymentService", SymbolKind.CLASS),
                        tuple("com.example.payments.PaymentService#charge", SymbolKind.METHOD),
                        tuple("com.example.payments.PaymentService.attempts", SymbolKind.FIELD),
                        tuple("com.example.payments.PaymentState", SymbolKind.ENUM)
                );
        assertThat(parsedProject.sourceFiles())
                .flatExtracting(ParsedSourceFile::dependencies)
                .extracting(ParsedDependency::targetName, ParsedDependency::kind)
                .contains(
                        tuple("List", DependencyKind.IMPORT),
                        tuple("Deprecated", DependencyKind.ANNOTATION),
                        tuple("BaseService", DependencyKind.EXTENDS),
                        tuple("PaymentPort", DependencyKind.IMPLEMENTS),
                        tuple("List<String>", DependencyKind.FIELD_TYPE),
                        tuple("recordAttempt", DependencyKind.METHOD_CALL)
                );
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.api.Assertions.tuple(values);
    }
}
