package de.mhus.nimbus.world.control.service.sync;

import de.mhus.nimbus.world.control.config.GitCredentialsProperties;
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
 * Falls back to GitCredentialsProperties from application.yaml if DTO doesn't provide credentials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHelper {

    private final RepositoryControl repositoryControl;
    private final GitCredentialsProperties gitProperties;

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
                getEffectiveBranch(definition),
                getEffectiveUsername(definition),
                getEffectivePassword(definition)
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
                getEffectiveUsername(definition),
                getEffectivePassword(definition)
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
                getEffectiveUsername(definition),
                getEffectivePassword(definition)
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

    /**
     * Validate Git configuration and connectivity.
     *
     * @param definition ExternalResource configuration
     * @return Validation result message
     */
    public String validate(ExternalResourceDTO definition) {
        Path localPath = Paths.get(definition.getLocalPath());

        String result = repositoryControl.validate(
                localPath,
                definition.getGitRepositoryUrl(),
                getEffectiveUsername(definition),
                getEffectivePassword(definition)
        );

        // Add credential source info
        StringBuilder enhanced = new StringBuilder(result);
        enhanced.append("\n=== Credential Source ===\n");

        if (definition.getGitUsername() != null && !definition.getGitUsername().isBlank()) {
            enhanced.append("Username: from ExternalResourceDTO\n");
        } else if (gitProperties.getUsername() != null) {
            enhanced.append("Username: from application.yaml (").append(gitProperties.getUsername()).append(")\n");
        }

        if (definition.getGitPassword() != null && !definition.getGitPassword().isBlank()) {
            enhanced.append("Password: from ExternalResourceDTO\n");
        } else if (gitProperties.getPassword() != null) {
            enhanced.append("Password: from application.yaml\n");
        }

        if (definition.getGitBranch() != null && !definition.getGitBranch().isBlank()) {
            enhanced.append("Branch: from ExternalResourceDTO (").append(definition.getGitBranch()).append(")\n");
        } else {
            enhanced.append("Branch: from application.yaml (").append(gitProperties.getBranch()).append(")\n");
        }

        return enhanced.toString();
    }

    /**
     * Get effective username (DTO or fallback to properties).
     */
    private String getEffectiveUsername(ExternalResourceDTO definition) {
        if (definition.getGitUsername() != null && !definition.getGitUsername().isBlank()) {
            return definition.getGitUsername();
        }
        return gitProperties.getUsername();
    }

    /**
     * Get effective password (DTO or fallback to properties).
     */
    private String getEffectivePassword(ExternalResourceDTO definition) {
        if (definition.getGitPassword() != null && !definition.getGitPassword().isBlank()) {
            return definition.getGitPassword();
        }
        return gitProperties.getPassword();
    }

    /**
     * Get effective branch (DTO or fallback to properties).
     */
    private String getEffectiveBranch(ExternalResourceDTO definition) {
        if (definition.getGitBranch() != null && !definition.getGitBranch().isBlank()) {
            return definition.getGitBranch();
        }
        return gitProperties.getBranch();
    }
}

