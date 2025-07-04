/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package de.mhus.nimbus.shared.avro;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

/** Schema für World-Deregistrierungs-Anfragen im Nimbus Registry System */
@org.apache.avro.specific.AvroGenerated
public class WorldUnregistrationRequest extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 1868002704273668905L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"WorldUnregistrationRequest\",\"namespace\":\"de.mhus.nimbus.shared.avro\",\"doc\":\"Schema für World-Deregistrierungs-Anfragen im Nimbus Registry System\",\"fields\":[{\"name\":\"requestId\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"Eindeutige ID der World-Deregistrierungs-Anfrage\"},{\"name\":\"worldId\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"ID der zu deregistrierenden Welt\"},{\"name\":\"planetName\",\"type\":[\"null\",{\"type\":\"string\",\"avro.java.string\":\"String\"}],\"doc\":\"Name des Planeten (optional, für zusätzliche Validierung)\",\"default\":null},{\"name\":\"environment\",\"type\":{\"type\":\"enum\",\"name\":\"Environment\",\"symbols\":[\"DEV\",\"TEST\",\"STAGING\",\"PROD\"]},\"doc\":\"Umgebung für die World-Deregistrierung\",\"default\":\"DEV\"},{\"name\":\"timestamp\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"},\"doc\":\"Zeitstempel der Anfrage in Millisekunden\"},{\"name\":\"unregisteredBy\",\"type\":[\"null\",{\"type\":\"string\",\"avro.java.string\":\"String\"}],\"doc\":\"Benutzer oder Service, der die Deregistrierung durchführt\",\"default\":null},{\"name\":\"reason\",\"type\":[\"null\",{\"type\":\"string\",\"avro.java.string\":\"String\"}],\"doc\":\"Grund für die Deregistrierung\",\"default\":null},{\"name\":\"metadata\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"avro.java.string\":\"String\"},\"doc\":\"Zusätzliche Metadaten für die Deregistrierungs-Anfrage\",\"default\":{}}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();
  static {
    MODEL$.addLogicalTypeConversion(new org.apache.avro.data.TimeConversions.TimestampMillisConversion());
  }

  private static final BinaryMessageEncoder<WorldUnregistrationRequest> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<WorldUnregistrationRequest> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<WorldUnregistrationRequest> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<WorldUnregistrationRequest> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<WorldUnregistrationRequest> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this WorldUnregistrationRequest to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a WorldUnregistrationRequest from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a WorldUnregistrationRequest instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static WorldUnregistrationRequest fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  /** Eindeutige ID der World-Deregistrierungs-Anfrage */
  private java.lang.String requestId;
  /** ID der zu deregistrierenden Welt */
  private java.lang.String worldId;
  /** Name des Planeten (optional, für zusätzliche Validierung) */
  private java.lang.String planetName;
  /** Umgebung für die World-Deregistrierung */
  private de.mhus.nimbus.shared.avro.Environment environment;
  /** Zeitstempel der Anfrage in Millisekunden */
  private java.time.Instant timestamp;
  /** Benutzer oder Service, der die Deregistrierung durchführt */
  private java.lang.String unregisteredBy;
  /** Grund für die Deregistrierung */
  private java.lang.String reason;
  /** Zusätzliche Metadaten für die Deregistrierungs-Anfrage */
  private java.util.Map<java.lang.String,java.lang.String> metadata;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public WorldUnregistrationRequest() {}

  /**
   * All-args constructor.
   * @param requestId Eindeutige ID der World-Deregistrierungs-Anfrage
   * @param worldId ID der zu deregistrierenden Welt
   * @param planetName Name des Planeten (optional, für zusätzliche Validierung)
   * @param environment Umgebung für die World-Deregistrierung
   * @param timestamp Zeitstempel der Anfrage in Millisekunden
   * @param unregisteredBy Benutzer oder Service, der die Deregistrierung durchführt
   * @param reason Grund für die Deregistrierung
   * @param metadata Zusätzliche Metadaten für die Deregistrierungs-Anfrage
   */
  public WorldUnregistrationRequest(java.lang.String requestId, java.lang.String worldId, java.lang.String planetName, de.mhus.nimbus.shared.avro.Environment environment, java.time.Instant timestamp, java.lang.String unregisteredBy, java.lang.String reason, java.util.Map<java.lang.String,java.lang.String> metadata) {
    this.requestId = requestId;
    this.worldId = worldId;
    this.planetName = planetName;
    this.environment = environment;
    this.timestamp = timestamp.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    this.unregisteredBy = unregisteredBy;
    this.reason = reason;
    this.metadata = metadata;
  }

  @Override
  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  // Used by DatumWriter.  Applications should not call.
  @Override
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return requestId;
    case 1: return worldId;
    case 2: return planetName;
    case 3: return environment;
    case 4: return timestamp;
    case 5: return unregisteredBy;
    case 6: return reason;
    case 7: return metadata;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  private static final org.apache.avro.Conversion<?>[] conversions =
      new org.apache.avro.Conversion<?>[] {
      null,
      null,
      null,
      null,
      new org.apache.avro.data.TimeConversions.TimestampMillisConversion(),
      null,
      null,
      null,
      null
  };

  @Override
  public org.apache.avro.Conversion<?> getConversion(int field) {
    return conversions[field];
  }

  // Used by DatumReader.  Applications should not call.
  @Override
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: requestId = value$ != null ? value$.toString() : null; break;
    case 1: worldId = value$ != null ? value$.toString() : null; break;
    case 2: planetName = value$ != null ? value$.toString() : null; break;
    case 3: environment = (de.mhus.nimbus.shared.avro.Environment)value$; break;
    case 4: timestamp = (java.time.Instant)value$; break;
    case 5: unregisteredBy = value$ != null ? value$.toString() : null; break;
    case 6: reason = value$ != null ? value$.toString() : null; break;
    case 7: metadata = (java.util.Map<java.lang.String,java.lang.String>)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'requestId' field.
   * @return Eindeutige ID der World-Deregistrierungs-Anfrage
   */
  public java.lang.String getRequestId() {
    return requestId;
  }



  /**
   * Gets the value of the 'worldId' field.
   * @return ID der zu deregistrierenden Welt
   */
  public java.lang.String getWorldId() {
    return worldId;
  }



  /**
   * Gets the value of the 'planetName' field.
   * @return Name des Planeten (optional, für zusätzliche Validierung)
   */
  public java.lang.String getPlanetName() {
    return planetName;
  }



  /**
   * Gets the value of the 'environment' field.
   * @return Umgebung für die World-Deregistrierung
   */
  public de.mhus.nimbus.shared.avro.Environment getEnvironment() {
    return environment;
  }



  /**
   * Gets the value of the 'timestamp' field.
   * @return Zeitstempel der Anfrage in Millisekunden
   */
  public java.time.Instant getTimestamp() {
    return timestamp;
  }



  /**
   * Gets the value of the 'unregisteredBy' field.
   * @return Benutzer oder Service, der die Deregistrierung durchführt
   */
  public java.lang.String getUnregisteredBy() {
    return unregisteredBy;
  }



  /**
   * Gets the value of the 'reason' field.
   * @return Grund für die Deregistrierung
   */
  public java.lang.String getReason() {
    return reason;
  }



  /**
   * Gets the value of the 'metadata' field.
   * @return Zusätzliche Metadaten für die Deregistrierungs-Anfrage
   */
  public java.util.Map<java.lang.String,java.lang.String> getMetadata() {
    return metadata;
  }



  /**
   * Creates a new WorldUnregistrationRequest RecordBuilder.
   * @return A new WorldUnregistrationRequest RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder newBuilder() {
    return new de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder();
  }

  /**
   * Creates a new WorldUnregistrationRequest RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new WorldUnregistrationRequest RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder newBuilder(de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder other) {
    if (other == null) {
      return new de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder();
    } else {
      return new de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder(other);
    }
  }

  /**
   * Creates a new WorldUnregistrationRequest RecordBuilder by copying an existing WorldUnregistrationRequest instance.
   * @param other The existing instance to copy.
   * @return A new WorldUnregistrationRequest RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder newBuilder(de.mhus.nimbus.shared.avro.WorldUnregistrationRequest other) {
    if (other == null) {
      return new de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder();
    } else {
      return new de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder(other);
    }
  }

  /**
   * RecordBuilder for WorldUnregistrationRequest instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<WorldUnregistrationRequest>
    implements org.apache.avro.data.RecordBuilder<WorldUnregistrationRequest> {

    /** Eindeutige ID der World-Deregistrierungs-Anfrage */
    private java.lang.String requestId;
    /** ID der zu deregistrierenden Welt */
    private java.lang.String worldId;
    /** Name des Planeten (optional, für zusätzliche Validierung) */
    private java.lang.String planetName;
    /** Umgebung für die World-Deregistrierung */
    private de.mhus.nimbus.shared.avro.Environment environment;
    /** Zeitstempel der Anfrage in Millisekunden */
    private java.time.Instant timestamp;
    /** Benutzer oder Service, der die Deregistrierung durchführt */
    private java.lang.String unregisteredBy;
    /** Grund für die Deregistrierung */
    private java.lang.String reason;
    /** Zusätzliche Metadaten für die Deregistrierungs-Anfrage */
    private java.util.Map<java.lang.String,java.lang.String> metadata;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.requestId)) {
        this.requestId = data().deepCopy(fields()[0].schema(), other.requestId);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.worldId)) {
        this.worldId = data().deepCopy(fields()[1].schema(), other.worldId);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.planetName)) {
        this.planetName = data().deepCopy(fields()[2].schema(), other.planetName);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
      if (isValidValue(fields()[3], other.environment)) {
        this.environment = data().deepCopy(fields()[3].schema(), other.environment);
        fieldSetFlags()[3] = other.fieldSetFlags()[3];
      }
      if (isValidValue(fields()[4], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[4].schema(), other.timestamp);
        fieldSetFlags()[4] = other.fieldSetFlags()[4];
      }
      if (isValidValue(fields()[5], other.unregisteredBy)) {
        this.unregisteredBy = data().deepCopy(fields()[5].schema(), other.unregisteredBy);
        fieldSetFlags()[5] = other.fieldSetFlags()[5];
      }
      if (isValidValue(fields()[6], other.reason)) {
        this.reason = data().deepCopy(fields()[6].schema(), other.reason);
        fieldSetFlags()[6] = other.fieldSetFlags()[6];
      }
      if (isValidValue(fields()[7], other.metadata)) {
        this.metadata = data().deepCopy(fields()[7].schema(), other.metadata);
        fieldSetFlags()[7] = other.fieldSetFlags()[7];
      }
    }

    /**
     * Creates a Builder by copying an existing WorldUnregistrationRequest instance
     * @param other The existing instance to copy.
     */
    private Builder(de.mhus.nimbus.shared.avro.WorldUnregistrationRequest other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.requestId)) {
        this.requestId = data().deepCopy(fields()[0].schema(), other.requestId);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.worldId)) {
        this.worldId = data().deepCopy(fields()[1].schema(), other.worldId);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.planetName)) {
        this.planetName = data().deepCopy(fields()[2].schema(), other.planetName);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.environment)) {
        this.environment = data().deepCopy(fields()[3].schema(), other.environment);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[4].schema(), other.timestamp);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.unregisteredBy)) {
        this.unregisteredBy = data().deepCopy(fields()[5].schema(), other.unregisteredBy);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.reason)) {
        this.reason = data().deepCopy(fields()[6].schema(), other.reason);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.metadata)) {
        this.metadata = data().deepCopy(fields()[7].schema(), other.metadata);
        fieldSetFlags()[7] = true;
      }
    }

    /**
      * Gets the value of the 'requestId' field.
      * Eindeutige ID der World-Deregistrierungs-Anfrage
      * @return The value.
      */
    public java.lang.String getRequestId() {
      return requestId;
    }


    /**
      * Sets the value of the 'requestId' field.
      * Eindeutige ID der World-Deregistrierungs-Anfrage
      * @param value The value of 'requestId'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder setRequestId(java.lang.String value) {
      validate(fields()[0], value);
      this.requestId = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'requestId' field has been set.
      * Eindeutige ID der World-Deregistrierungs-Anfrage
      * @return True if the 'requestId' field has been set, false otherwise.
      */
    public boolean hasRequestId() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'requestId' field.
      * Eindeutige ID der World-Deregistrierungs-Anfrage
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder clearRequestId() {
      requestId = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'worldId' field.
      * ID der zu deregistrierenden Welt
      * @return The value.
      */
    public java.lang.String getWorldId() {
      return worldId;
    }


    /**
      * Sets the value of the 'worldId' field.
      * ID der zu deregistrierenden Welt
      * @param value The value of 'worldId'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder setWorldId(java.lang.String value) {
      validate(fields()[1], value);
      this.worldId = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'worldId' field has been set.
      * ID der zu deregistrierenden Welt
      * @return True if the 'worldId' field has been set, false otherwise.
      */
    public boolean hasWorldId() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'worldId' field.
      * ID der zu deregistrierenden Welt
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder clearWorldId() {
      worldId = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'planetName' field.
      * Name des Planeten (optional, für zusätzliche Validierung)
      * @return The value.
      */
    public java.lang.String getPlanetName() {
      return planetName;
    }


    /**
      * Sets the value of the 'planetName' field.
      * Name des Planeten (optional, für zusätzliche Validierung)
      * @param value The value of 'planetName'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder setPlanetName(java.lang.String value) {
      validate(fields()[2], value);
      this.planetName = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'planetName' field has been set.
      * Name des Planeten (optional, für zusätzliche Validierung)
      * @return True if the 'planetName' field has been set, false otherwise.
      */
    public boolean hasPlanetName() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'planetName' field.
      * Name des Planeten (optional, für zusätzliche Validierung)
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder clearPlanetName() {
      planetName = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'environment' field.
      * Umgebung für die World-Deregistrierung
      * @return The value.
      */
    public de.mhus.nimbus.shared.avro.Environment getEnvironment() {
      return environment;
    }


    /**
      * Sets the value of the 'environment' field.
      * Umgebung für die World-Deregistrierung
      * @param value The value of 'environment'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder setEnvironment(de.mhus.nimbus.shared.avro.Environment value) {
      validate(fields()[3], value);
      this.environment = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'environment' field has been set.
      * Umgebung für die World-Deregistrierung
      * @return True if the 'environment' field has been set, false otherwise.
      */
    public boolean hasEnvironment() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'environment' field.
      * Umgebung für die World-Deregistrierung
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder clearEnvironment() {
      environment = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'timestamp' field.
      * Zeitstempel der Anfrage in Millisekunden
      * @return The value.
      */
    public java.time.Instant getTimestamp() {
      return timestamp;
    }


    /**
      * Sets the value of the 'timestamp' field.
      * Zeitstempel der Anfrage in Millisekunden
      * @param value The value of 'timestamp'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder setTimestamp(java.time.Instant value) {
      validate(fields()[4], value);
      this.timestamp = value.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'timestamp' field has been set.
      * Zeitstempel der Anfrage in Millisekunden
      * @return True if the 'timestamp' field has been set, false otherwise.
      */
    public boolean hasTimestamp() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'timestamp' field.
      * Zeitstempel der Anfrage in Millisekunden
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder clearTimestamp() {
      fieldSetFlags()[4] = false;
      return this;
    }

    /**
      * Gets the value of the 'unregisteredBy' field.
      * Benutzer oder Service, der die Deregistrierung durchführt
      * @return The value.
      */
    public java.lang.String getUnregisteredBy() {
      return unregisteredBy;
    }


    /**
      * Sets the value of the 'unregisteredBy' field.
      * Benutzer oder Service, der die Deregistrierung durchführt
      * @param value The value of 'unregisteredBy'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder setUnregisteredBy(java.lang.String value) {
      validate(fields()[5], value);
      this.unregisteredBy = value;
      fieldSetFlags()[5] = true;
      return this;
    }

    /**
      * Checks whether the 'unregisteredBy' field has been set.
      * Benutzer oder Service, der die Deregistrierung durchführt
      * @return True if the 'unregisteredBy' field has been set, false otherwise.
      */
    public boolean hasUnregisteredBy() {
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'unregisteredBy' field.
      * Benutzer oder Service, der die Deregistrierung durchführt
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder clearUnregisteredBy() {
      unregisteredBy = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /**
      * Gets the value of the 'reason' field.
      * Grund für die Deregistrierung
      * @return The value.
      */
    public java.lang.String getReason() {
      return reason;
    }


    /**
      * Sets the value of the 'reason' field.
      * Grund für die Deregistrierung
      * @param value The value of 'reason'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder setReason(java.lang.String value) {
      validate(fields()[6], value);
      this.reason = value;
      fieldSetFlags()[6] = true;
      return this;
    }

    /**
      * Checks whether the 'reason' field has been set.
      * Grund für die Deregistrierung
      * @return True if the 'reason' field has been set, false otherwise.
      */
    public boolean hasReason() {
      return fieldSetFlags()[6];
    }


    /**
      * Clears the value of the 'reason' field.
      * Grund für die Deregistrierung
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder clearReason() {
      reason = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /**
      * Gets the value of the 'metadata' field.
      * Zusätzliche Metadaten für die Deregistrierungs-Anfrage
      * @return The value.
      */
    public java.util.Map<java.lang.String,java.lang.String> getMetadata() {
      return metadata;
    }


    /**
      * Sets the value of the 'metadata' field.
      * Zusätzliche Metadaten für die Deregistrierungs-Anfrage
      * @param value The value of 'metadata'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder setMetadata(java.util.Map<java.lang.String,java.lang.String> value) {
      validate(fields()[7], value);
      this.metadata = value;
      fieldSetFlags()[7] = true;
      return this;
    }

    /**
      * Checks whether the 'metadata' field has been set.
      * Zusätzliche Metadaten für die Deregistrierungs-Anfrage
      * @return True if the 'metadata' field has been set, false otherwise.
      */
    public boolean hasMetadata() {
      return fieldSetFlags()[7];
    }


    /**
      * Clears the value of the 'metadata' field.
      * Zusätzliche Metadaten für die Deregistrierungs-Anfrage
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.WorldUnregistrationRequest.Builder clearMetadata() {
      metadata = null;
      fieldSetFlags()[7] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public WorldUnregistrationRequest build() {
      try {
        WorldUnregistrationRequest record = new WorldUnregistrationRequest();
        record.requestId = fieldSetFlags()[0] ? this.requestId : (java.lang.String) defaultValue(fields()[0]);
        record.worldId = fieldSetFlags()[1] ? this.worldId : (java.lang.String) defaultValue(fields()[1]);
        record.planetName = fieldSetFlags()[2] ? this.planetName : (java.lang.String) defaultValue(fields()[2]);
        record.environment = fieldSetFlags()[3] ? this.environment : (de.mhus.nimbus.shared.avro.Environment) defaultValue(fields()[3]);
        record.timestamp = fieldSetFlags()[4] ? this.timestamp : (java.time.Instant) defaultValue(fields()[4]);
        record.unregisteredBy = fieldSetFlags()[5] ? this.unregisteredBy : (java.lang.String) defaultValue(fields()[5]);
        record.reason = fieldSetFlags()[6] ? this.reason : (java.lang.String) defaultValue(fields()[6]);
        record.metadata = fieldSetFlags()[7] ? this.metadata : (java.util.Map<java.lang.String,java.lang.String>) defaultValue(fields()[7]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<WorldUnregistrationRequest>
    WRITER$ = (org.apache.avro.io.DatumWriter<WorldUnregistrationRequest>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<WorldUnregistrationRequest>
    READER$ = (org.apache.avro.io.DatumReader<WorldUnregistrationRequest>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}










