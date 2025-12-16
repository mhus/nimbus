package de.mhus.nimbus.world.shared.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for job management.
 * Provides CRUD operations and job state transitions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WJobService {

    private final WJobRepository jobRepository;
    private final JobExecutorRegistry executorRegistry;

    @Transactional
    public WJob createJob(String worldId, String executor, String type,
                          Map<String, String> parameters) {
        return createJob(worldId, executor, type, parameters, 5, 0);
    }

    @Transactional
    public WJob createJob(String worldId, String executor, String type,
                          Map<String, String> parameters, int priority, int maxRetries) {

        if (!executorRegistry.hasExecutor(executor)) {
            log.warn("Creating job with unknown executor: {}", executor);
        }

        WJob job = WJob.builder()
                .worldId(worldId)
                .executor(executor)
                .type(type)
                .status(JobStatus.PENDING.name())
                .parameters(parameters != null ? parameters : Map.of())
                .priority(priority)
                .maxRetries(maxRetries)
                .build();

        job.touchCreate();
        WJob saved = jobRepository.save(job);

        log.info("Created job: id={} world={} executor={} type={} priority={}",
                saved.getId(), worldId, executor, type, priority);

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<WJob> getJob(String jobId) {
        return jobRepository.findById(jobId);
    }

    @Transactional(readOnly = true)
    public List<WJob> getJobsByWorld(String worldId) {
        return jobRepository.findByWorldId(worldId);
    }

    @Transactional(readOnly = true)
    public List<WJob> getJobsByWorldAndStatus(String worldId, JobStatus status) {
        return jobRepository.findByWorldIdAndStatus(worldId, status.name());
    }

    @Transactional(readOnly = true)
    public List<WJob> getPendingJobs() {
        return jobRepository.findByStatusAndEnabledOrderByPriorityDescCreatedAtAsc(
                JobStatus.PENDING.name(), true);
    }

    @Transactional
    public Optional<WJob> markJobRunning(String jobId) {
        return jobRepository.findById(jobId).map(job -> {
            job.markStarted();
            WJob saved = jobRepository.save(job);
            log.debug("Job started: id={} world={} executor={}",
                    jobId, job.getWorldId(), job.getExecutor());
            return saved;
        });
    }

    @Transactional
    public Optional<WJob> markJobCompleted(String jobId, String result) {
        return jobRepository.findById(jobId).map(job -> {
            job.markCompleted(result);
            WJob saved = jobRepository.save(job);
            log.info("Job completed: id={} world={} executor={} duration={}ms",
                    jobId, job.getWorldId(), job.getExecutor(),
                    calculateDuration(job));
            return saved;
        });
    }

    @Transactional
    public Optional<WJob> markJobFailed(String jobId, String errorMessage) {
        return jobRepository.findById(jobId).map(job -> {
            job.markFailed(errorMessage);

            if (job.canRetry()) {
                job.setStatus(JobStatus.PENDING.name());
                job.setStartedAt(null);
                log.info("Job failed, retrying: id={} world={} executor={} retry={}/{} error={}",
                        jobId, job.getWorldId(), job.getExecutor(),
                        job.getRetryCount(), job.getMaxRetries(), errorMessage);
            } else {
                log.error("Job failed: id={} world={} executor={} error={}",
                        jobId, job.getWorldId(), job.getExecutor(), errorMessage);
            }

            return jobRepository.save(job);
        });
    }

    @Transactional
    public Optional<WJob> updateJob(String jobId, Consumer<WJob> updater) {
        return jobRepository.findById(jobId).map(job -> {
            updater.accept(job);
            job.touchUpdate();
            return jobRepository.save(job);
        });
    }

    @Transactional
    public boolean deleteJob(String jobId) {
        return jobRepository.findById(jobId).map(job -> {
            job.setEnabled(false);
            job.touchUpdate();
            jobRepository.save(job);
            log.debug("Job soft-deleted: id={}", jobId);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean hardDeleteJob(String jobId) {
        if (jobRepository.existsById(jobId)) {
            jobRepository.deleteById(jobId);
            log.debug("Job hard-deleted: id={}", jobId);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<WJob> findJobsForCleanup(Instant cutoffTime) {
        return jobRepository.findByStatusInAndCompletedAtBefore(
                List.of(JobStatus.COMPLETED.name(), JobStatus.FAILED.name()),
                cutoffTime
        );
    }

    @Transactional(readOnly = true)
    public long countJobs(String worldId, JobStatus status) {
        return jobRepository.countByWorldIdAndStatus(worldId, status.name());
    }

    private Long calculateDuration(WJob job) {
        if (job.getStartedAt() != null && job.getCompletedAt() != null) {
            return job.getCompletedAt().toEpochMilli() - job.getStartedAt().toEpochMilli();
        }
        return null;
    }
}
