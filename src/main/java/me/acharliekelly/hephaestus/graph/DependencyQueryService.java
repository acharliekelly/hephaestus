package me.acharliekelly.hephaestus.graph;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import me.acharliekelly.hephaestus.model.CodeSymbolRecord;
import me.acharliekelly.hephaestus.model.DependencyKind;
import me.acharliekelly.hephaestus.model.DependencyRecord;
import me.acharliekelly.hephaestus.model.persistence.CodeSymbolRecordRepository;
import me.acharliekelly.hephaestus.model.persistence.DependencyRecordRepository;
import me.acharliekelly.hephaestus.repo.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DependencyQueryService {
    private final CodeSymbolRecordRepository symbols;
    private final DependencyRecordRepository dependencies;

    public DependencyQueryService(
            CodeSymbolRecordRepository symbols,
            DependencyRecordRepository dependencies
    ) {
        this.symbols = symbols;
        this.dependencies = dependencies;
    }

    @Transactional(readOnly = true)
    public List<CodeSymbolRecord> findSymbols(Long repositoryId, String name) {
        String query = name == null ? "" : name;
        return symbols.findByRepositoryIdAndNameContainingIgnoreCase(repositoryId, query);
    }

    @Transactional(readOnly = true)
    public List<DependencyRecord> dependenciesOf(Long symbolId) {
        requireSymbol(symbolId);
        return dependencies.findByFromSymbolId(symbolId);
    }

    @Transactional(readOnly = true)
    public List<DependencyRecord> dependentsOf(Long symbolId) {
        requireSymbol(symbolId);
        return dependencies.findByTargetSymbolId(symbolId);
    }

    @Transactional(readOnly = true)
    public List<CodeSymbolRecord> implementationsOf(Long interfaceSymbolId) {
        requireSymbol(interfaceSymbolId);
        return dependencies.findByTargetSymbolIdAndKind(interfaceSymbolId, DependencyKind.IMPLEMENTS).stream()
                .map(DependencyRecord::getFromSymbol)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(CodeSymbolRecord::getQualifiedName))
                .toList();
    }

    private void requireSymbol(Long symbolId) {
        if (!symbols.existsById(symbolId)) {
            throw new NotFoundException("Symbol " + symbolId + " was not found");
        }
    }
}
