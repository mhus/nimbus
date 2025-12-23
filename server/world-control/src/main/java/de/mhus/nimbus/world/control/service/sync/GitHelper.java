package de.mhus.nimbus.world.control.service.sync;

import de.mhus.nimbus.world.shared.dto.ExternalResourceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper service for Git operations using RepositoryControl.
 * Handles Git sync based on ExternalResourceDTO configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHelper {

    private final RepositoryControl repositoryControl;

    /**
     * Initialize or clone repository if needed.
     *
     * @param definition ExternalResource configuration
     * @throws IOException if init/clone fails
     */
    public void initOrClone(ExternalResourceDTO definition) throws IOException {
        if (!definition.isAutoGit()) {
            return;
        }

        Path localPath = Paths.get(definition.getLocalPath());

        repositoryControl.initOrClone(
                localPath,
                definition.getGitRepositoryUrl(),
                definition.getGitBranch(),
                definition.getGitUsername(),
                definition.getGitPassword()
        );
    }

    /**
     * Pull latest changes from remote.
     * Uses hard reset before pull to ensure clean state.
     *
     * @param definition ExternalResource configuration
     * @throws IOException if pull fails
     */
    public void pull(ExternalResourceDTO definition) throws IOException {
        if (!definition.isAutoGit()) {
            return;
        }

        Path localPath = Paths.get(definition.getLocalPath());

        // Ensure repository exists
        if (!repositoryControl.isGitRepository(localPath)) {
            log.warn("Not a git repository, skipping pull: {}", localPath);
            return;
        }

        repositoryControl.pull(
                localPath,
                definition.getGitUsername(),
                definition.getGitPassword()
        );
    }

    /**
     * Commit all changes and push to remote.
     *
     * @param definition ExternalResource configuration
     * @param message    Commit message
     * @throws IOException if commit/push fails
     */
    public void commitAndPush(ExternalResourceDTO definition, String message) throws IOException {
        if (!definition.isAutoGit()) {
            return;
        }

        Path localPath = Paths.get(definition.getLocalPath());

        // Ensure repository exists
        if (!repositoryControl.isGitRepository(localPath)) {
            log.warn("Not a git repository, skipping commit/push: {}", localPath);
            return;
        }

        repositoryControl.commitAndPush(
                localPath,
                message,
                definition.getGitUsername(),
                definition.getGitPassword()
        );
    }

    /**
     * Reset repository to clean state.
     *
     * @param definition ExternalResource configuration
     * @throws IOException if reset fails
     */
    public void resetHard(ExternalResourceDTO definition) throws IOException {
        if (!definition.isAutoGit()) {
            return;
        }

        Path localPath = Paths.get(definition.getLocalPath());

        if (!repositoryControl.isGitRepository(localPath)) {
            log.warn("Not a git repository, skipping reset: {}", localPath);
            return;
        }

        repositoryControl.resetHard(localPath);
    }
}
