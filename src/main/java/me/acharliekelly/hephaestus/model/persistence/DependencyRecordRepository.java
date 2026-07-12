package me.acharliekelly.hephaestus.model.persistence;

import java.util.List;
import me.acharliekelly.hephaestus.model.DependencyKind;
import me.acharliekelly.hephaestus.model.DependencyRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DependencyRecordRepository extends JpaRepository<DependencyRecord, Long> {
    @EntityGraph(attributePaths = {"sourceFile", "fromSymbol", "targetSymbol"})
    List<DependencyRecord> findByFromSymbolId(Long symbolId);

    @EntityGraph(attributePaths = {"sourceFile", "fromSymbol", "targetSymbol"})
    List<DependencyRecord> findByTargetSymbolId(Long symbolId);

    @EntityGraph(attributePaths = {"sourceFile", "fromSymbol", "fromSymbol.sourceFile", "targetSymbol", "targetSymbol.sourceFile"})
    List<DependencyRecord> findByTargetSymbolIdAndKind(Long symbolId, DependencyKind kind);

    @EntityGraph(attributePaths = {"sourceFile", "fromSymbol", "fromSymbol.sourceFile", "targetSymbol", "targetSymbol.sourceFile"})
    List<DependencyRecord> findByRepositoryId(Long repositoryId);

    @Modifying
    @Query("delete from DependencyRecord dependency where dependency.repository.id = :repositoryId")
    void deleteByRepositoryId(Long repositoryId);
}
