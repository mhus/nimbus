/*
 * Source TS: sample-types.ts
 * Original TS: 'interface Person'
 */
package de.mhus.nimbus.evaluate.generated;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Person {
    @Deprecated
    @SuppressWarnings("required")
    private String id;
    @Deprecated
    @SuppressWarnings("optional")
    private String name;
}
