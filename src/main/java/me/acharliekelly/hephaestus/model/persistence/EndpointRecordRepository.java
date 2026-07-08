package me.acharliekelly.hephaestus.model.persistence;

import java.util.List;
import me.acharliekelly.hephaestus.model.EndpointRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EndpointRecordRepository extends JpaRepository<EndpointRecord, Long> {
    @EntityGraph(attributePaths = {"sourceFile", "controllerSymbol", "handlerSymbol"})
    List<EndpointRecord> findByRepositoryId(Long repositoryId);

    @Modifying
    @Query("delete from EndpointRecord endpoint where endpoint.repository.id = :repositoryId")
    void deleteByRepositoryId(Long repositoryId);
}
