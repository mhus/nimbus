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

/** Schema für PlayerCharacter-Lookup-Antworten im Nimbus Identity System */
@org.apache.avro.specific.AvroGenerated
public class PlayerCharacterLookupResponse extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 5823395860584520463L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"PlayerCharacterLookupResponse\",\"namespace\":\"de.mhus.nimbus.shared.avro\",\"doc\":\"Schema für PlayerCharacter-Lookup-Antworten im Nimbus Identity System\",\"fields\":[{\"name\":\"requestId\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"ID der ursprünglichen PlayerCharacter-Lookup-Anfrage\"},{\"name\":\"status\",\"type\":{\"type\":\"enum\",\"name\":\"PlayerCharacterLookupStatus\",\"symbols\":[\"SUCCESS\",\"CHARACTER_NOT_FOUND\",\"USER_NOT_FOUND\",\"ERROR\",\"TIMEOUT\"]},\"doc\":\"Status der PlayerCharacter-Lookup-Anfrage\"},{\"name\":\"characters\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"PlayerCharacterInfo\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"name\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"description\",\"type\":[\"null\",{\"type\":\"string\",\"avro.java.string\":\"String\"}],\"default\":null},{\"name\":\"characterClass\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"level\",\"type\":\"int\",\"default\":1},{\"name\":\"experiencePoints\",\"type\":\"long\",\"default\":0},{\"name\":\"healthPoints\",\"type\":\"int\",\"default\":100},{\"name\":\"maxHealthPoints\",\"type\":\"int\",\"default\":100},{\"name\":\"manaPoints\",\"type\":\"int\",\"default\":100},{\"name\":\"maxManaPoints\",\"type\":\"int\",\"default\":100},{\"name\":\"currentWorldId\",\"type\":[\"null\",{\"type\":\"string\",\"avro.java.string\":\"String\"}],\"default\":null},{\"name\":\"currentPlanet\",\"type\":[\"null\",{\"type\":\"string\",\"avro.java.string\":\"String\"}],\"default\":null},{\"name\":\"positionX\",\"type\":\"double\",\"default\":0.0},{\"name\":\"positionY\",\"type\":\"double\",\"default\":0.0},{\"name\":\"positionZ\",\"type\":\"double\",\"default\":0.0},{\"name\":\"active\",\"type\":\"boolean\",\"default\":true},{\"name\":\"lastLogin\",\"type\":[\"null\",{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"}],\"default\":null},{\"name\":\"createdAt\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"}},{\"name\":\"updatedAt\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"}},{\"name\":\"userId\",\"type\":\"long\"}]}},\"doc\":\"Liste der gefundenen PlayerCharacters\",\"default\":[]},{\"name\":\"timestamp\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"},\"doc\":\"Zeitstempel der Antwort\"},{\"name\":\"errorMessage\",\"type\":[\"null\",{\"type\":\"string\",\"avro.java.string\":\"String\"}],\"doc\":\"Fehlermeldung bei ERROR-Status\",\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();
  static {
    MODEL$.addLogicalTypeConversion(new org.apache.avro.data.TimeConversions.TimestampMillisConversion());
  }

  private static final BinaryMessageEncoder<PlayerCharacterLookupResponse> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<PlayerCharacterLookupResponse> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<PlayerCharacterLookupResponse> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<PlayerCharacterLookupResponse> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<PlayerCharacterLookupResponse> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this PlayerCharacterLookupResponse to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a PlayerCharacterLookupResponse from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a PlayerCharacterLookupResponse instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static PlayerCharacterLookupResponse fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  /** ID der ursprünglichen PlayerCharacter-Lookup-Anfrage */
  private java.lang.String requestId;
  /** Status der PlayerCharacter-Lookup-Anfrage */
  private de.mhus.nimbus.shared.avro.PlayerCharacterLookupStatus status;
  /** Liste der gefundenen PlayerCharacters */
  private java.util.List<de.mhus.nimbus.shared.avro.PlayerCharacterInfo> characters;
  /** Zeitstempel der Antwort */
  private java.time.Instant timestamp;
  /** Fehlermeldung bei ERROR-Status */
  private java.lang.String errorMessage;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public PlayerCharacterLookupResponse() {}

  /**
   * All-args constructor.
   * @param requestId ID der ursprünglichen PlayerCharacter-Lookup-Anfrage
   * @param status Status der PlayerCharacter-Lookup-Anfrage
   * @param characters Liste der gefundenen PlayerCharacters
   * @param timestamp Zeitstempel der Antwort
   * @param errorMessage Fehlermeldung bei ERROR-Status
   */
  public PlayerCharacterLookupResponse(java.lang.String requestId, de.mhus.nimbus.shared.avro.PlayerCharacterLookupStatus status, java.util.List<de.mhus.nimbus.shared.avro.PlayerCharacterInfo> characters, java.time.Instant timestamp, java.lang.String errorMessage) {
    this.requestId = requestId;
    this.status = status;
    this.characters = characters;
    this.timestamp = timestamp.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    this.errorMessage = errorMessage;
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
    case 1: return status;
    case 2: return characters;
    case 3: return timestamp;
    case 4: return errorMessage;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  private static final org.apache.avro.Conversion<?>[] conversions =
      new org.apache.avro.Conversion<?>[] {
      null,
      null,
      null,
      new org.apache.avro.data.TimeConversions.TimestampMillisConversion(),
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
    case 1: status = (de.mhus.nimbus.shared.avro.PlayerCharacterLookupStatus)value$; break;
    case 2: characters = (java.util.List<de.mhus.nimbus.shared.avro.PlayerCharacterInfo>)value$; break;
    case 3: timestamp = (java.time.Instant)value$; break;
    case 4: errorMessage = value$ != null ? value$.toString() : null; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'requestId' field.
   * @return ID der ursprünglichen PlayerCharacter-Lookup-Anfrage
   */
  public java.lang.String getRequestId() {
    return requestId;
  }



  /**
   * Gets the value of the 'status' field.
   * @return Status der PlayerCharacter-Lookup-Anfrage
   */
  public de.mhus.nimbus.shared.avro.PlayerCharacterLookupStatus getStatus() {
    return status;
  }



  /**
   * Gets the value of the 'characters' field.
   * @return Liste der gefundenen PlayerCharacters
   */
  public java.util.List<de.mhus.nimbus.shared.avro.PlayerCharacterInfo> getCharacters() {
    return characters;
  }



  /**
   * Gets the value of the 'timestamp' field.
   * @return Zeitstempel der Antwort
   */
  public java.time.Instant getTimestamp() {
    return timestamp;
  }



  /**
   * Gets the value of the 'errorMessage' field.
   * @return Fehlermeldung bei ERROR-Status
   */
  public java.lang.String getErrorMessage() {
    return errorMessage;
  }



  /**
   * Creates a new PlayerCharacterLookupResponse RecordBuilder.
   * @return A new PlayerCharacterLookupResponse RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder newBuilder() {
    return new de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder();
  }

  /**
   * Creates a new PlayerCharacterLookupResponse RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new PlayerCharacterLookupResponse RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder newBuilder(de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder other) {
    if (other == null) {
      return new de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder();
    } else {
      return new de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder(other);
    }
  }

  /**
   * Creates a new PlayerCharacterLookupResponse RecordBuilder by copying an existing PlayerCharacterLookupResponse instance.
   * @param other The existing instance to copy.
   * @return A new PlayerCharacterLookupResponse RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder newBuilder(de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse other) {
    if (other == null) {
      return new de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder();
    } else {
      return new de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder(other);
    }
  }

  /**
   * RecordBuilder for PlayerCharacterLookupResponse instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<PlayerCharacterLookupResponse>
    implements org.apache.avro.data.RecordBuilder<PlayerCharacterLookupResponse> {

    /** ID der ursprünglichen PlayerCharacter-Lookup-Anfrage */
    private java.lang.String requestId;
    /** Status der PlayerCharacter-Lookup-Anfrage */
    private de.mhus.nimbus.shared.avro.PlayerCharacterLookupStatus status;
    /** Liste der gefundenen PlayerCharacters */
    private java.util.List<de.mhus.nimbus.shared.avro.PlayerCharacterInfo> characters;
    /** Zeitstempel der Antwort */
    private java.time.Instant timestamp;
    /** Fehlermeldung bei ERROR-Status */
    private java.lang.String errorMessage;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.requestId)) {
        this.requestId = data().deepCopy(fields()[0].schema(), other.requestId);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.status)) {
        this.status = data().deepCopy(fields()[1].schema(), other.status);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.characters)) {
        this.characters = data().deepCopy(fields()[2].schema(), other.characters);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
      if (isValidValue(fields()[3], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[3].schema(), other.timestamp);
        fieldSetFlags()[3] = other.fieldSetFlags()[3];
      }
      if (isValidValue(fields()[4], other.errorMessage)) {
        this.errorMessage = data().deepCopy(fields()[4].schema(), other.errorMessage);
        fieldSetFlags()[4] = other.fieldSetFlags()[4];
      }
    }

    /**
     * Creates a Builder by copying an existing PlayerCharacterLookupResponse instance
     * @param other The existing instance to copy.
     */
    private Builder(de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.requestId)) {
        this.requestId = data().deepCopy(fields()[0].schema(), other.requestId);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.status)) {
        this.status = data().deepCopy(fields()[1].schema(), other.status);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.characters)) {
        this.characters = data().deepCopy(fields()[2].schema(), other.characters);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[3].schema(), other.timestamp);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.errorMessage)) {
        this.errorMessage = data().deepCopy(fields()[4].schema(), other.errorMessage);
        fieldSetFlags()[4] = true;
      }
    }

    /**
      * Gets the value of the 'requestId' field.
      * ID der ursprünglichen PlayerCharacter-Lookup-Anfrage
      * @return The value.
      */
    public java.lang.String getRequestId() {
      return requestId;
    }


    /**
      * Sets the value of the 'requestId' field.
      * ID der ursprünglichen PlayerCharacter-Lookup-Anfrage
      * @param value The value of 'requestId'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder setRequestId(java.lang.String value) {
      validate(fields()[0], value);
      this.requestId = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'requestId' field has been set.
      * ID der ursprünglichen PlayerCharacter-Lookup-Anfrage
      * @return True if the 'requestId' field has been set, false otherwise.
      */
    public boolean hasRequestId() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'requestId' field.
      * ID der ursprünglichen PlayerCharacter-Lookup-Anfrage
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder clearRequestId() {
      requestId = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'status' field.
      * Status der PlayerCharacter-Lookup-Anfrage
      * @return The value.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupStatus getStatus() {
      return status;
    }


    /**
      * Sets the value of the 'status' field.
      * Status der PlayerCharacter-Lookup-Anfrage
      * @param value The value of 'status'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder setStatus(de.mhus.nimbus.shared.avro.PlayerCharacterLookupStatus value) {
      validate(fields()[1], value);
      this.status = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'status' field has been set.
      * Status der PlayerCharacter-Lookup-Anfrage
      * @return True if the 'status' field has been set, false otherwise.
      */
    public boolean hasStatus() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'status' field.
      * Status der PlayerCharacter-Lookup-Anfrage
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder clearStatus() {
      status = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'characters' field.
      * Liste der gefundenen PlayerCharacters
      * @return The value.
      */
    public java.util.List<de.mhus.nimbus.shared.avro.PlayerCharacterInfo> getCharacters() {
      return characters;
    }


    /**
      * Sets the value of the 'characters' field.
      * Liste der gefundenen PlayerCharacters
      * @param value The value of 'characters'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder setCharacters(java.util.List<de.mhus.nimbus.shared.avro.PlayerCharacterInfo> value) {
      validate(fields()[2], value);
      this.characters = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'characters' field has been set.
      * Liste der gefundenen PlayerCharacters
      * @return True if the 'characters' field has been set, false otherwise.
      */
    public boolean hasCharacters() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'characters' field.
      * Liste der gefundenen PlayerCharacters
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder clearCharacters() {
      characters = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'timestamp' field.
      * Zeitstempel der Antwort
      * @return The value.
      */
    public java.time.Instant getTimestamp() {
      return timestamp;
    }


    /**
      * Sets the value of the 'timestamp' field.
      * Zeitstempel der Antwort
      * @param value The value of 'timestamp'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder setTimestamp(java.time.Instant value) {
      validate(fields()[3], value);
      this.timestamp = value.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'timestamp' field has been set.
      * Zeitstempel der Antwort
      * @return True if the 'timestamp' field has been set, false otherwise.
      */
    public boolean hasTimestamp() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'timestamp' field.
      * Zeitstempel der Antwort
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder clearTimestamp() {
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'errorMessage' field.
      * Fehlermeldung bei ERROR-Status
      * @return The value.
      */
    public java.lang.String getErrorMessage() {
      return errorMessage;
    }


    /**
      * Sets the value of the 'errorMessage' field.
      * Fehlermeldung bei ERROR-Status
      * @param value The value of 'errorMessage'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder setErrorMessage(java.lang.String value) {
      validate(fields()[4], value);
      this.errorMessage = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'errorMessage' field has been set.
      * Fehlermeldung bei ERROR-Status
      * @return True if the 'errorMessage' field has been set, false otherwise.
      */
    public boolean hasErrorMessage() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'errorMessage' field.
      * Fehlermeldung bei ERROR-Status
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse.Builder clearErrorMessage() {
      errorMessage = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PlayerCharacterLookupResponse build() {
      try {
        PlayerCharacterLookupResponse record = new PlayerCharacterLookupResponse();
        record.requestId = fieldSetFlags()[0] ? this.requestId : (java.lang.String) defaultValue(fields()[0]);
        record.status = fieldSetFlags()[1] ? this.status : (de.mhus.nimbus.shared.avro.PlayerCharacterLookupStatus) defaultValue(fields()[1]);
        record.characters = fieldSetFlags()[2] ? this.characters : (java.util.List<de.mhus.nimbus.shared.avro.PlayerCharacterInfo>) defaultValue(fields()[2]);
        record.timestamp = fieldSetFlags()[3] ? this.timestamp : (java.time.Instant) defaultValue(fields()[3]);
        record.errorMessage = fieldSetFlags()[4] ? this.errorMessage : (java.lang.String) defaultValue(fields()[4]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<PlayerCharacterLookupResponse>
    WRITER$ = (org.apache.avro.io.DatumWriter<PlayerCharacterLookupResponse>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<PlayerCharacterLookupResponse>
    READER$ = (org.apache.avro.io.DatumReader<PlayerCharacterLookupResponse>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}










