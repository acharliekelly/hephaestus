package me.acharliekelly.hephaestus.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
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
                        tuple("com.example.payments.PaymentService#charge()", SymbolKind.METHOD),
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

    @Test
    void qualifiesOverloadedMethodsWithParameterTypes(@TempDir Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example"));
        Files.writeString(packageDir.resolve("ParserService.java"), """
                package com.example;

                public class ParserService {
                    String parseFilename(String fileName) {
                        return parseFilename(fileName, "-");
                    }

                    String parseFilename(String fileName, String separator) {
                        return fileName + separator;
                    }
                }
                """);

        ParsedProject parsedProject = javaParserService.parse(projectRoot);

        assertThat(parsedProject.sourceFiles())
                .flatExtracting(ParsedSourceFile::symbols)
                .extracting(ParsedSymbol::qualifiedName, ParsedSymbol::kind)
                .contains(
                        tuple("com.example.ParserService#parseFilename(String)", SymbolKind.METHOD),
                        tuple("com.example.ParserService#parseFilename(String,String)", SymbolKind.METHOD)
                );
        assertThat(parsedProject.sourceFiles())
                .flatExtracting(ParsedSourceFile::dependencies)
                .filteredOn(dependency -> dependency.kind() == DependencyKind.METHOD_CALL)
                .extracting(ParsedDependency::fromSymbolQualifiedName, ParsedDependency::targetName)
                .contains(tuple("com.example.ParserService#parseFilename(String)", "parseFilename"));
    }

    @Test
    void parsesSpringMvcEndpoints(@TempDir Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/web"));
        Files.writeString(packageDir.resolve("FileController.java"), """
                package com.example.web;

                import org.springframework.web.bind.annotation.DeleteMapping;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/api/files")
                public class FileController {
                    @GetMapping
                    public String list() {
                        return "ok";
                    }

                    @PostMapping({"/upload", "/imports"})
                    public String upload(@RequestBody String body) {
                        return body;
                    }

                    @DeleteMapping("/{id}")
                    public void delete(@PathVariable String id) {
                    }
                }
                """);

        ParsedProject parsedProject = javaParserService.parse(projectRoot);

        assertThat(parsedProject.sourceFiles())
                .flatExtracting(ParsedSourceFile::endpoints)
                .extracting(
                        ParsedEndpoint::httpMethod,
                        ParsedEndpoint::path,
                        ParsedEndpoint::controllerQualifiedName,
                        ParsedEndpoint::handlerQualifiedName
                )
                .contains(
                        tuple("GET", "/api/files", "com.example.web.FileController",
                                "com.example.web.FileController#list()"),
                        tuple("POST", "/api/files/upload", "com.example.web.FileController",
                                "com.example.web.FileController#upload(String)"),
                        tuple("POST", "/api/files/imports", "com.example.web.FileController",
                                "com.example.web.FileController#upload(String)"),
                        tuple("DELETE", "/api/files/{id}", "com.example.web.FileController",
                                "com.example.web.FileController#delete(String)")
                );
    }

    @Test
    void parsesJava21RecordTextBlockAndSwitchExpression(@TempDir Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/payments"));
        Files.writeString(packageDir.resolve("PaymentCommand.java"), """
                package com.example.payments;

                public record PaymentCommand(String accountId, int cents) {
                    public String description() {
                        return \"""
                                payment:%s:%d
                                \""".formatted(accountId, cents);
                    }
                }
                """);
        Files.writeString(packageDir.resolve("PaymentService.java"), """
                package com.example.payments;

                public class PaymentService {
                    public String describe(PaymentCommand command) {
                        return switch (command.cents()) {
                            case 0 -> "free";
                            default -> command.description();
                        };
                    }
                }
                """);

        ParsedProject parsedProject = javaParserService.parse(projectRoot);

        assertThat(parsedProject.sourceFiles()).hasSize(2);
        assertThat(parsedProject.sourceFiles())
                .flatExtracting(ParsedSourceFile::symbols)
                .extracting(ParsedSymbol::qualifiedName, ParsedSymbol::kind)
                .contains(
                        tuple("com.example.payments.PaymentCommand", SymbolKind.CLASS),
                        tuple("com.example.payments.PaymentCommand#description()", SymbolKind.METHOD),
                        tuple("com.example.payments.PaymentService", SymbolKind.CLASS),
                        tuple("com.example.payments.PaymentService#describe(PaymentCommand)", SymbolKind.METHOD)
                );
        assertThat(parsedProject.sourceFiles())
                .flatExtracting(ParsedSourceFile::dependencies)
                .filteredOn(dependency -> dependency.kind() == DependencyKind.METHOD_CALL)
                .extracting(ParsedDependency::fromSymbolQualifiedName, ParsedDependency::targetName)
                .contains(
                        tuple("com.example.payments.PaymentCommand#description()", "formatted"),
                        tuple("com.example.payments.PaymentService#describe(PaymentCommand)", "description")
                );
    }

    @Test
    void parsesJava21SourceFromWorkerThread(@TempDir Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example"));
        Files.writeString(packageDir.resolve("PaymentCommand.java"), """
                package com.example;

                public record PaymentCommand(String accountId, int cents) {
                    public String description() {
                        return \"""
                                payment:%s:%d
                                \""".formatted(accountId, cents);
                    }
                }
                """);

        try (var executor = Executors.newSingleThreadExecutor()) {
            ParsedProject parsedProject;
            try {
                parsedProject = executor.submit(() -> javaParserService.parse(projectRoot)).get();
            } catch (ExecutionException ex) {
                throw (Exception) ex.getCause();
            }

            assertThat(parsedProject.sourceFiles())
                    .flatExtracting(ParsedSourceFile::symbols)
                    .extracting(ParsedSymbol::qualifiedName, ParsedSymbol::kind)
                    .contains(tuple("com.example.PaymentCommand", SymbolKind.CLASS));
        }
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.api.Assertions.tuple(values);
    }
}
