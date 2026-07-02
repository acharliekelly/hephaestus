package me.acharliekelly.hephaestus.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import me.acharliekelly.hephaestus.model.RepositoryRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RepoServiceTest {
    @Autowired
    private RepoService repoService;

    @Test
    void importsAnExistingAbsoluteDirectory(@TempDir Path projectRoot) throws Exception {
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());

        assertThat(repository.getId()).isNotNull();
        assertThat(repository.getCanonicalPath()).isEqualTo(projectRoot.toRealPath().toString());
        assertThat(repository.getName()).isEqualTo(projectRoot.getFileName().toString());
    }

    @Test
    void returnsExistingRepositoryForSameCanonicalPath(@TempDir Path projectRoot) {
        RepositoryRecord first = repoService.importLocal(projectRoot.toString());
        RepositoryRecord second = repoService.importLocal(projectRoot.toString());

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    void rejectsRelativePaths() {
        assertThatThrownBy(() -> repoService.importLocal("relative/path"))
                .isInstanceOf(InvalidRepositoryPathException.class)
                .hasMessageContaining("absolute");
    }

    @Test
    void rejectsFiles(@TempDir Path projectRoot) throws Exception {
        Path file = Files.writeString(projectRoot.resolve("not-a-directory.txt"), "content");

        assertThatThrownBy(() -> repoService.importLocal(file.toString()))
                .isInstanceOf(InvalidRepositoryPathException.class)
                .hasMessageContaining("directory");
    }
}

