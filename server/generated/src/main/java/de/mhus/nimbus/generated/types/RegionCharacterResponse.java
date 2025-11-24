/*
 * Source TS: RegionCharacterResponse.ts
 * Original TS: 'interface RegionCharacterResponse'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class RegionCharacterResponse {
    private String id;
    private String userId;
    private String regionId;
    private String name;
    private String display;
    private java.util.Map<String, RegionItemInfo> backpack;
    private java.util.Map<java.lang.Double, RegionItemInfo> wearing;
    private java.util.Map<String, java.lang.Double> skills;
}
