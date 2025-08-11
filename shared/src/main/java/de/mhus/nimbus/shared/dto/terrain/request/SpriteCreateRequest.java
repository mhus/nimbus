package de.mhus.nimbus.shared.dto.terrain.request;

import de.mhus.nimbus.shared.dto.terrain.SpriteDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpriteCreateRequest {
    private String world;
    private Integer level;
    private List<SpriteData> sprites;

    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class SpriteData extends SpriteDto {
        private Boolean dynamic;
    }
}
