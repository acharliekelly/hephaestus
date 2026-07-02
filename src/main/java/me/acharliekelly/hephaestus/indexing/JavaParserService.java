package me.acharliekelly.hephaestus.indexing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import me.acharliekelly.hephaestus.model.DependencyKind;
import me.acharliekelly.hephaestus.model.SymbolKind;
import org.springframework.stereotype.Service;

@Service
public class JavaParserService {
    public ParsedProject parse(Path projectRoot) {
        try (var paths = Files.walk(projectRoot)) {
            List<ParsedSourceFile> sourceFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(path -> parseSourceFile(projectRoot, path))
                    .toList();
            return new ParsedProject(sourceFiles);
        } catch (IOException ex) {
            throw new IndexingException("Failed to scan Java sources under " + projectRoot, ex);
        }
    }

    private ParsedSourceFile parseSourceFile(Path projectRoot, Path sourceFile) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(sourceFile);
            String packageName = compilationUnit.getPackageDeclaration()
                    .map(packageDeclaration -> packageDeclaration.getName().asString())
                    .orElse("");
            List<ParsedSymbol> symbols = new ArrayList<>();
            List<ParsedDependency> dependencies = new ArrayList<>();

            compilationUnit.getImports().forEach(importDeclaration -> {
                String importedName = importDeclaration.getName().asString();
                dependencies.add(new ParsedDependency(
                        null,
                        simpleName(importedName),
                        importDeclaration.isAsterisk() ? null : importedName,
                        DependencyKind.IMPORT,
                        line(importDeclaration)
                ));
            });

            compilationUnit.findAll(TypeDeclaration.class).forEach(typeDeclaration -> {
                String typeQualifiedName = qualifiedTypeName(packageName, typeDeclaration);
                symbols.add(new ParsedSymbol(
                        typeDeclaration.getNameAsString(),
                        typeQualifiedName,
                        symbolKind(typeDeclaration),
                        line(typeDeclaration)
                ));
                addTypeDependencies(typeDeclaration, typeQualifiedName, dependencies);
                addAnnotationDependencies(typeDeclaration, typeQualifiedName, dependencies);
            });

            compilationUnit.findAll(FieldDeclaration.class).forEach(fieldDeclaration -> {
                Optional<TypeDeclaration<?>> owner = ownerType(fieldDeclaration);
                owner.ifPresent(typeDeclaration -> fieldDeclaration.getVariables().forEach(variable -> {
                    String ownerQualifiedName = qualifiedTypeName(packageName, typeDeclaration);
                    String fieldQualifiedName = ownerQualifiedName + "." + variable.getNameAsString();
                    symbols.add(new ParsedSymbol(
                            variable.getNameAsString(),
                            fieldQualifiedName,
                            SymbolKind.FIELD,
                            line(variable)
                    ));
                    dependencies.add(new ParsedDependency(
                            ownerQualifiedName,
                            variable.getType().asString(),
                            null,
                            DependencyKind.FIELD_TYPE,
                            line(variable)
                    ));
                }));
            });

            compilationUnit.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
                Optional<TypeDeclaration<?>> owner = ownerType(methodDeclaration);
                owner.ifPresent(typeDeclaration -> {
                    String ownerQualifiedName = qualifiedTypeName(packageName, typeDeclaration);
                    String methodQualifiedName = ownerQualifiedName + "#" + methodDeclaration.getNameAsString();
                    symbols.add(new ParsedSymbol(
                            methodDeclaration.getNameAsString(),
                            methodQualifiedName,
                            SymbolKind.METHOD,
                            line(methodDeclaration)
                    ));
                    addAnnotationDependencies(methodDeclaration, methodQualifiedName, dependencies);
                    methodDeclaration.findAll(MethodCallExpr.class).forEach(methodCall -> dependencies.add(new ParsedDependency(
                            methodQualifiedName,
                            methodCall.getNameAsString(),
                            null,
                            DependencyKind.METHOD_CALL,
                            line(methodCall)
                    )));
                });
            });

            return new ParsedSourceFile(
                    projectRoot.relativize(sourceFile).toString(),
                    sourceFile.toAbsolutePath().normalize().toString(),
                    packageName,
                    symbols,
                    dependencies
            );
        } catch (IOException ex) {
            throw new IndexingException("Failed to parse " + sourceFile, ex);
        }
    }

    private void addTypeDependencies(
            TypeDeclaration<?> typeDeclaration,
            String typeQualifiedName,
            List<ParsedDependency> dependencies
    ) {
        if (typeDeclaration instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
            classOrInterfaceDeclaration.getExtendedTypes().forEach(extendedType -> dependencies.add(typeDependency(
                    typeQualifiedName,
                    extendedType,
                    DependencyKind.EXTENDS
            )));
            classOrInterfaceDeclaration.getImplementedTypes().forEach(implementedType -> dependencies.add(typeDependency(
                    typeQualifiedName,
                    implementedType,
                    DependencyKind.IMPLEMENTS
            )));
        }
    }

    private ParsedDependency typeDependency(
            String typeQualifiedName,
            ClassOrInterfaceType targetType,
            DependencyKind kind
    ) {
        return new ParsedDependency(
                typeQualifiedName,
                targetType.getNameAsString(),
                targetType.getNameWithScope(),
                kind,
                line(targetType)
        );
    }

    private void addAnnotationDependencies(
            NodeWithAnnotations<?> node,
            String fromSymbolQualifiedName,
            List<ParsedDependency> dependencies
    ) {
        node.getAnnotations().forEach(annotation -> dependencies.add(new ParsedDependency(
                fromSymbolQualifiedName,
                annotationName(annotation),
                annotationName(annotation),
                DependencyKind.ANNOTATION,
                line(annotation)
        )));
    }

    private SymbolKind symbolKind(TypeDeclaration<?> typeDeclaration) {
        if (typeDeclaration instanceof EnumDeclaration) {
            return SymbolKind.ENUM;
        }
        if (typeDeclaration instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration
                && classOrInterfaceDeclaration.isInterface()) {
            return SymbolKind.INTERFACE;
        }
        return SymbolKind.CLASS;
    }

    @SuppressWarnings("unchecked")
    private Optional<TypeDeclaration<?>> ownerType(Node node) {
        return node.findAncestor(TypeDeclaration.class)
                .map(typeDeclaration -> (TypeDeclaration<?>) typeDeclaration);
    }

    private String qualifiedTypeName(String packageName, TypeDeclaration<?> typeDeclaration) {
        String localName = typeDeclaration.getFullyQualifiedName()
                .orElse(typeDeclaration.getNameAsString());
        if (localName.contains(".")) {
            return localName;
        }
        return packageName.isBlank() ? localName : packageName + "." + localName;
    }

    private String annotationName(AnnotationExpr annotation) {
        return annotation.getName().asString();
    }

    private String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    private Integer line(Node node) {
        return node.getBegin().map(position -> position.line).orElse(null);
    }
}
