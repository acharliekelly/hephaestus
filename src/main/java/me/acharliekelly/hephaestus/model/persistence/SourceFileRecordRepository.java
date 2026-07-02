package me.acharliekelly.hephaestus.model.persistence;

import java.util.List;
import me.acharliekelly.hephaestus.model.SourceFileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SourceFileRecordRepository extends JpaRepository<SourceFileRecord, Long> {
    List<SourceFileRecord> findByRepositoryId(Long repositoryId);

    @Modifying
    @Query("delete from SourceFileRecord sourceFile where sourceFile.repository.id = :repositoryId")
    void deleteByRepositoryId(Long repositoryId);
}
