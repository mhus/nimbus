package de.mhus.nimbus.world.control.service.sync;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Git repository control implementation using JGit.
 * Provides robust Git operations with hard reset strategy for data safety.
 */
@Service
@Slf4j
public class RepositoryControlWithJGit implements RepositoryControl {

    @Override
    public void initOrClone(Path localPath, String repositoryUrl, String branch, String username, String password) throws IOException {
        if (isGitRepository(localPath)) {
            log.info("Git repository already exists: {}", localPath);
            return;
        }

        if (repositoryUrl != null && !repositoryUrl.isBlank()) {
            // Clone repository
            log.info("Cloning repository: {} to {}", repositoryUrl, localPath);
            try {
                var cloneCommand = Git.cloneRepository()
                        .setURI(repositoryUrl)
                        .setDirectory(localPath.toFile())
                        .setCloneAllBranches(false);

                if (branch != null && !branch.isBlank()) {
                    cloneCommand.setBranch(branch);
                }

                if (username != null && !username.isBlank() && password != null) {
                    cloneCommand.setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(username, password)
                    );
                }

                try (Git git = cloneCommand.call()) {
                    log.info("Repository cloned successfully: {}", localPath);
                }
            } catch (GitAPIException e) {
                throw new IOException("Failed to clone repository: " + e.getMessage(), e);
            }
        } else if (Files.exists(localPath)) {
            // Initialize existing directory as git repo
            log.info("Initializing git repository: {}", localPath);
            try {
                try (Git git = Git.init().setDirectory(localPath.toFile()).call()) {
                    log.info("Git repository initialized: {}", localPath);
                }
            } catch (GitAPIException e) {
                throw new IOException("Failed to initialize repository: " + e.getMessage(), e);
            }
        } else {
            throw new IOException("Cannot init/clone: path doesn't exist and no repositoryUrl provided");
        }
    }

    @Override
    public void pull(Path localPath, String username, String password) throws IOException {
        log.info("Pulling from remote: {}", localPath);

        try (Git git = openRepository(localPath)) {
            // First: reset hard to clean state
            resetHard(localPath);

            // Then: pull with rebase
            var pullCommand = git.pull()
                    .setRebase(true);

            if (username != null && !username.isBlank() && password != null) {
                pullCommand.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(username, password)
                );
            }

            var result = pullCommand.call();

            if (result.isSuccessful()) {
                log.info("Pull successful: {}", localPath);
            } else {
                log.warn("Pull completed with issues: {}", result);
            }
        } catch (GitAPIException e) {
            throw new IOException("Failed to pull: " + e.getMessage(), e);
        }
    }

    @Override
    public void commitAndPush(Path localPath, String message, String username, String password) throws IOException {
        log.info("Committing and pushing: {}", localPath);

        try (Git git = openRepository(localPath)) {
            // Add all changes
            git.add()
                    .addFilepattern(".")
                    .call();

            // Check if there are changes to commit
            var status = git.status().call();
            if (status.isClean()) {
                log.info("No changes to commit: {}", localPath);
                return;
            }

            // Commit
            git.commit()
                    .setMessage(message)
                    .setAllowEmpty(false)
                    .call();

            log.info("Changes committed: {}", localPath);

            // Push
            var pushCommand = git.push();

            if (username != null && !username.isBlank() && password != null) {
                pushCommand.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(username, password)
                );
            }

            pushCommand.call();
            log.info("Changes pushed to remote: {}", localPath);

        } catch (GitAPIException e) {
            throw new IOException("Failed to commit/push: " + e.getMessage(), e);
        }
    }

    @Override
    public void resetHard(Path localPath) throws IOException {
        log.info("Resetting repository to HEAD (hard): {}", localPath);

        try (Git git = openRepository(localPath)) {
            git.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef("HEAD")
                    .call();

            log.info("Repository reset successful: {}", localPath);
        } catch (GitAPIException e) {
            throw new IOException("Failed to reset repository: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isGitRepository(Path localPath) {
        if (!Files.exists(localPath)) {
            return false;
        }

        File gitDir = localPath.resolve(".git").toFile();
        return gitDir.exists() && gitDir.isDirectory();
    }

    /**
     * Open existing Git repository.
     */
    private Git openRepository(Path localPath) throws IOException {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .setGitDir(localPath.resolve(".git").toFile())
                    .readEnvironment()
                    .findGitDir()
                    .build();

            return new Git(repository);
        } catch (IOException e) {
            throw new IOException("Failed to open Git repository: " + localPath, e);
        }
    }
}
