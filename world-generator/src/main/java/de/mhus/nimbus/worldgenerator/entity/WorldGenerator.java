package de.mhus.nimbus.worldgenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "world_generator")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldGenerator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String worldId;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @ElementCollection
    @CollectionTable(name = "world_generator_parameters",
                    joinColumns = @JoinColumn(name = "world_generator_id"))
    @MapKeyColumn(name = "parameter_key")
    @Column(name = "parameter_value", length = 2000)
    private Map<String, String> parameters;

    @OneToMany(mappedBy = "worldGenerator", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorldGeneratorPhase> phases;

    @Column
    private Integer totalPhases;

    @Column
    private Integer completedPhases;

    @Column
    private String currentPhase;

    @Column(length = 2000)
    private String errorMessage;

    public enum GenerationStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = GenerationStatus.PENDING;
        }
    }

    public double getProgressPercentage() {
        if (totalPhases == null || totalPhases == 0) {
            return 0.0;
        }
        return (double) (completedPhases != null ? completedPhases : 0) / totalPhases * 100.0;
    }
}
