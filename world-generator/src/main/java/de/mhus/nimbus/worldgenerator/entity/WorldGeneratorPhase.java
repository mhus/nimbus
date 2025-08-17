package de.mhus.nimbus.worldgenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "world_generator_phase")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldGeneratorPhase {

    // Phase Type Konstanten
    public static final String PHASE_INITIALIZATION = "INITIALIZATION";
    public static final String PHASE_ASSET_MATERIAL_GENERATION = "ASSET_MATERIAL_GENERATION";
    public static final String PHASE_CONTINENT_GENERATION = "CONTINENT_GENERATION";
    public static final String PHASE_TERRAIN_GENERATION = "TERRAIN_GENERATION";
    public static final String PHASE_HISTORICAL_GENERATION = "HISTORICAL_GENERATION";
    public static final String PHASE_STRUCTURE_GENERATION = "STRUCTURE_GENERATION";
    public static final String PHASE_ITEM_GENERATION = "ITEM_GENERATION";
    public static final String PHASE_QUEST_GENERATION = "QUEST_GENERATION";

    // Display Names für die Phasen (deutsch)
    public static final Map<String, String> PHASE_DISPLAY_NAMES = Map.of(
        PHASE_INITIALIZATION, "Initialisierung",
        PHASE_ASSET_MATERIAL_GENERATION, "Asset/Material-Generierung",
        PHASE_CONTINENT_GENERATION, "Kontinent-Generierung",
        PHASE_TERRAIN_GENERATION, "Terrain-Generierung",
        PHASE_HISTORICAL_GENERATION, "Historische Generierung",
        PHASE_STRUCTURE_GENERATION, "Struktur-Generierung",
        PHASE_ITEM_GENERATION, "Item-Generierung",
        PHASE_QUEST_GENERATION, "Quest-Generierung"
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_generator_id", nullable = false)
    private WorldGenerator worldGenerator;

    @Column(nullable = false, name = "phase_type")
    private String phaseType;

    @Column(nullable = false)
    private Integer phaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @ElementCollection
    @CollectionTable(name = "world_generator_phase_parameters",
                    joinColumns = @JoinColumn(name = "phase_id"))
    @MapKeyColumn(name = "parameter_key")
    @Column(name = "parameter_value", length = 2000)
    private Map<String, String> parameters;

    @Column
    private Integer progressPercentage;

    @Column(length = 2000)
    private String errorMessage;

    @Column(length = 1000)
    private String resultSummary;

    @Column
    private Long estimatedDurationMinutes;

    @Column
    private Long actualDurationMinutes;

    public enum PhaseStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        SKIPPED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PhaseStatus.PENDING;
        }
        if (progressPercentage == null) {
            progressPercentage = 0;
        }
    }

    public Long getActualDurationMinutes() {
        if (startedAt != null && completedAt != null) {
            return java.time.Duration.between(startedAt, completedAt).toMinutes();
        }
        return actualDurationMinutes;
    }

    public String getPhaseDisplayName() {
        return PHASE_DISPLAY_NAMES.getOrDefault(phaseType, phaseType);
    }

    // Hilfsmethoden für häufig verwendete Phasen
    public static String[] getDefaultPhaseTypes() {
        return new String[]{
            PHASE_INITIALIZATION,
            PHASE_ASSET_MATERIAL_GENERATION,
            PHASE_CONTINENT_GENERATION,
            PHASE_TERRAIN_GENERATION,
            PHASE_HISTORICAL_GENERATION,
            PHASE_STRUCTURE_GENERATION,
            PHASE_ITEM_GENERATION,
            PHASE_QUEST_GENERATION
        };
    }
}
