/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package de.mhus.nimbus.shared.avro;
@org.apache.avro.specific.AvroGenerated
public enum PlayerCharacterLookupStatus implements org.apache.avro.generic.GenericEnumSymbol<PlayerCharacterLookupStatus> {
  SUCCESS, CHARACTER_NOT_FOUND, USER_NOT_FOUND, ERROR, TIMEOUT  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"PlayerCharacterLookupStatus\",\"namespace\":\"de.mhus.nimbus.shared.avro\",\"symbols\":[\"SUCCESS\",\"CHARACTER_NOT_FOUND\",\"USER_NOT_FOUND\",\"ERROR\",\"TIMEOUT\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
}
