package de.mhus.nimbus.shared.dto.region;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionCharacterResponse {
    private String id;
    private String userId;
    private String name;
    private String display;
    private Map<String, RegionItemInfo> backpack;
    private Map<Integer, RegionItemInfo> wearing;
    private Map<String, Integer> skills;
    private String regionId; // neu: Region, zu der der Charakter gehört
}
