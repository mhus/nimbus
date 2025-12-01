/*
 * Source TS: ScrawlTypes.ts
 * Original TS: 'interface ScrawlSubject'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScrawlSubject {
    private de.mhus.nimbus.evaluate.generated.types.Vector3 position;
    private String entityId;
    private String blockId;
}
