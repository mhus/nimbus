package de.mhus.nimbus.world.control.service.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper service for Git operations.
 * Executes git commands using ProcessBuilder.
 */
@Service
@Slf4j
public class GitHelper {

    /**
     * Execute git pull with rebase.
     *
     * @param repoPath Path to git repository
     * @throws IOException if git command fails
     */
    public void pull(Path repoPath) throws IOException {
        log.info("Git pull in: {}", repoPath);
        executeGit(repoPath, "pull", "--rebase");
    }

    /**
     * Commit all changes and push to remote.
     *
     * @param repoPath Path to git repository
     * @param message  Commit message
     * @throws IOException if git command fails
     */
    public void commitAndPush(Path repoPath, String message) throws IOException {
        log.info("Git commit and push in: {}", repoPath);

        // Add all changes
        executeGit(repoPath, "add", ".");

        // Commit (may fail if no changes)
        try {
            executeGit(repoPath, "commit", "-m", message);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("nothing to commit")) {
                log.info("No changes to commit");
                return;
            }
            throw e;
        }

        // Push
        executeGit(repoPath, "push");
    }

    /**
     * Execute a git command.
     *
     * @param repoPath Path to git repository
     * @param commands Git commands and arguments
     * @throws IOException if command fails
     */
    private void executeGit(Path repoPath, String... commands) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(Arrays.asList(commands));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(repoPath.toFile());
        pb.redirectErrorStream(true);

        log.debug("Executing: {} in {}", String.join(" ", cmd), repoPath);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new IOException("Failed to start git process: " + e.getMessage(), e);
        }

        // Read output
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }

        // Wait for completion
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git process interrupted", e);
        }

        if (exitCode != 0) {
            throw new IOException("Git command failed (exit code " + exitCode + "): " + output);
        }

        log.debug("Git command successful: {}", output);
    }
}
