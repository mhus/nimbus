/*
 * Source TS: ScrawlTypes.ts
 * Original TS: 'interface ScrawlSubject'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScrawlSubject {
    @Deprecated
    @SuppressWarnings("required")
    private de.mhus.nimbus.evaluate.generated.types.Vector3 position;
    @Deprecated
    @SuppressWarnings("optional")
    private String entityId;
    @Deprecated
    @SuppressWarnings("optional")
    private String blockId;
}
