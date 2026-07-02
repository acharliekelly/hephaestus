package me.acharliekelly.hephaestus.model.persistence;

import java.util.List;
import me.acharliekelly.hephaestus.model.CodeSymbolRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CodeSymbolRecordRepository extends JpaRepository<CodeSymbolRecord, Long> {
    @EntityGraph(attributePaths = {"sourceFile"})
    List<CodeSymbolRecord> findByRepositoryIdAndNameContainingIgnoreCase(Long repositoryId, String name);

    List<CodeSymbolRecord> findByRepositoryId(Long repositoryId);

    @Modifying
    @Query("delete from CodeSymbolRecord symbol where symbol.repository.id = :repositoryId")
    void deleteByRepositoryId(Long repositoryId);
}
