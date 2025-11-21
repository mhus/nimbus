/*
 * Source TS: sample-types.ts
 * Original TS: 'interface Employee'
 */
package de.mhus.nimbus.generated;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Employee extends Person {
    private double employeeId;
}
