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

/** Schema für Lookup-Antworten im Nimbus Registry System */
@org.apache.avro.specific.AvroGenerated
public class LookupResponse extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -197023078740587957L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"LookupResponse\",\"namespace\":\"de.mhus.nimbus.shared.avro\",\"doc\":\"Schema für Lookup-Antworten im Nimbus Registry System\",\"fields\":[{\"name\":\"requestId\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"ID der ursprünglichen Lookup-Anfrage\"},{\"name\":\"status\",\"type\":{\"type\":\"enum\",\"name\":\"LookupStatus\",\"symbols\":[\"SUCCESS\",\"NOT_FOUND\",\"ERROR\",\"TIMEOUT\"]},\"doc\":\"Status der Lookup-Anfrage\"},{\"name\":\"service\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"Name des gesuchten Services\"},{\"name\":\"serviceInstances\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"ServiceInstance\",\"fields\":[{\"name\":\"instanceId\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"Eindeutige ID der Service-Instanz\"},{\"name\":\"host\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"Hostname oder IP-Adresse\"},{\"name\":\"port\",\"type\":\"int\",\"doc\":\"Port der Service-Instanz\"},{\"name\":\"version\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"Version der Service-Instanz\"},{\"name\":\"healthy\",\"type\":\"boolean\",\"doc\":\"Gesundheitsstatus der Instanz\",\"default\":true},{\"name\":\"lastHealthCheck\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"},\"doc\":\"Zeitstempel der letzten Gesundheitsprüfung\"},{\"name\":\"metadata\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"avro.java.string\":\"String\"},\"doc\":\"Zusätzliche Metadaten der Service-Instanz\",\"default\":{}}]}},\"doc\":\"Liste der gefundenen Service-Instanzen\",\"default\":[]},{\"name\":\"timestamp\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"},\"doc\":\"Zeitstempel der Antwort\"},{\"name\":\"errorMessage\",\"type\":[\"null\",{\"type\":\"string\",\"avro.java.string\":\"String\"}],\"doc\":\"Fehlermeldung bei ERROR-Status\",\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();
  static {
    MODEL$.addLogicalTypeConversion(new org.apache.avro.data.TimeConversions.TimestampMillisConversion());
  }

  private static final BinaryMessageEncoder<LookupResponse> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<LookupResponse> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<LookupResponse> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<LookupResponse> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<LookupResponse> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this LookupResponse to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a LookupResponse from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a LookupResponse instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static LookupResponse fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  /** ID der ursprünglichen Lookup-Anfrage */
  private java.lang.String requestId;
  /** Status der Lookup-Anfrage */
  private de.mhus.nimbus.shared.avro.LookupStatus status;
  /** Name des gesuchten Services */
  private java.lang.String service;
  /** Liste der gefundenen Service-Instanzen */
  private java.util.List<de.mhus.nimbus.shared.avro.ServiceInstance> serviceInstances;
  /** Zeitstempel der Antwort */
  private java.time.Instant timestamp;
  /** Fehlermeldung bei ERROR-Status */
  private java.lang.String errorMessage;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public LookupResponse() {}

  /**
   * All-args constructor.
   * @param requestId ID der ursprünglichen Lookup-Anfrage
   * @param status Status der Lookup-Anfrage
   * @param service Name des gesuchten Services
   * @param serviceInstances Liste der gefundenen Service-Instanzen
   * @param timestamp Zeitstempel der Antwort
   * @param errorMessage Fehlermeldung bei ERROR-Status
   */
  public LookupResponse(java.lang.String requestId, de.mhus.nimbus.shared.avro.LookupStatus status, java.lang.String service, java.util.List<de.mhus.nimbus.shared.avro.ServiceInstance> serviceInstances, java.time.Instant timestamp, java.lang.String errorMessage) {
    this.requestId = requestId;
    this.status = status;
    this.service = service;
    this.serviceInstances = serviceInstances;
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
    case 2: return service;
    case 3: return serviceInstances;
    case 4: return timestamp;
    case 5: return errorMessage;
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
    case 1: status = (de.mhus.nimbus.shared.avro.LookupStatus)value$; break;
    case 2: service = value$ != null ? value$.toString() : null; break;
    case 3: serviceInstances = (java.util.List<de.mhus.nimbus.shared.avro.ServiceInstance>)value$; break;
    case 4: timestamp = (java.time.Instant)value$; break;
    case 5: errorMessage = value$ != null ? value$.toString() : null; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'requestId' field.
   * @return ID der ursprünglichen Lookup-Anfrage
   */
  public java.lang.String getRequestId() {
    return requestId;
  }



  /**
   * Gets the value of the 'status' field.
   * @return Status der Lookup-Anfrage
   */
  public de.mhus.nimbus.shared.avro.LookupStatus getStatus() {
    return status;
  }



  /**
   * Gets the value of the 'service' field.
   * @return Name des gesuchten Services
   */
  public java.lang.String getService() {
    return service;
  }



  /**
   * Gets the value of the 'serviceInstances' field.
   * @return Liste der gefundenen Service-Instanzen
   */
  public java.util.List<de.mhus.nimbus.shared.avro.ServiceInstance> getServiceInstances() {
    return serviceInstances;
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
   * Creates a new LookupResponse RecordBuilder.
   * @return A new LookupResponse RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.LookupResponse.Builder newBuilder() {
    return new de.mhus.nimbus.shared.avro.LookupResponse.Builder();
  }

  /**
   * Creates a new LookupResponse RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new LookupResponse RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.LookupResponse.Builder newBuilder(de.mhus.nimbus.shared.avro.LookupResponse.Builder other) {
    if (other == null) {
      return new de.mhus.nimbus.shared.avro.LookupResponse.Builder();
    } else {
      return new de.mhus.nimbus.shared.avro.LookupResponse.Builder(other);
    }
  }

  /**
   * Creates a new LookupResponse RecordBuilder by copying an existing LookupResponse instance.
   * @param other The existing instance to copy.
   * @return A new LookupResponse RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.LookupResponse.Builder newBuilder(de.mhus.nimbus.shared.avro.LookupResponse other) {
    if (other == null) {
      return new de.mhus.nimbus.shared.avro.LookupResponse.Builder();
    } else {
      return new de.mhus.nimbus.shared.avro.LookupResponse.Builder(other);
    }
  }

  /**
   * RecordBuilder for LookupResponse instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<LookupResponse>
    implements org.apache.avro.data.RecordBuilder<LookupResponse> {

    /** ID der ursprünglichen Lookup-Anfrage */
    private java.lang.String requestId;
    /** Status der Lookup-Anfrage */
    private de.mhus.nimbus.shared.avro.LookupStatus status;
    /** Name des gesuchten Services */
    private java.lang.String service;
    /** Liste der gefundenen Service-Instanzen */
    private java.util.List<de.mhus.nimbus.shared.avro.ServiceInstance> serviceInstances;
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
    private Builder(de.mhus.nimbus.shared.avro.LookupResponse.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.requestId)) {
        this.requestId = data().deepCopy(fields()[0].schema(), other.requestId);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.status)) {
        this.status = data().deepCopy(fields()[1].schema(), other.status);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.service)) {
        this.service = data().deepCopy(fields()[2].schema(), other.service);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
      if (isValidValue(fields()[3], other.serviceInstances)) {
        this.serviceInstances = data().deepCopy(fields()[3].schema(), other.serviceInstances);
        fieldSetFlags()[3] = other.fieldSetFlags()[3];
      }
      if (isValidValue(fields()[4], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[4].schema(), other.timestamp);
        fieldSetFlags()[4] = other.fieldSetFlags()[4];
      }
      if (isValidValue(fields()[5], other.errorMessage)) {
        this.errorMessage = data().deepCopy(fields()[5].schema(), other.errorMessage);
        fieldSetFlags()[5] = other.fieldSetFlags()[5];
      }
    }

    /**
     * Creates a Builder by copying an existing LookupResponse instance
     * @param other The existing instance to copy.
     */
    private Builder(de.mhus.nimbus.shared.avro.LookupResponse other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.requestId)) {
        this.requestId = data().deepCopy(fields()[0].schema(), other.requestId);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.status)) {
        this.status = data().deepCopy(fields()[1].schema(), other.status);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.service)) {
        this.service = data().deepCopy(fields()[2].schema(), other.service);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.serviceInstances)) {
        this.serviceInstances = data().deepCopy(fields()[3].schema(), other.serviceInstances);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[4].schema(), other.timestamp);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.errorMessage)) {
        this.errorMessage = data().deepCopy(fields()[5].schema(), other.errorMessage);
        fieldSetFlags()[5] = true;
      }
    }

    /**
      * Gets the value of the 'requestId' field.
      * ID der ursprünglichen Lookup-Anfrage
      * @return The value.
      */
    public java.lang.String getRequestId() {
      return requestId;
    }


    /**
      * Sets the value of the 'requestId' field.
      * ID der ursprünglichen Lookup-Anfrage
      * @param value The value of 'requestId'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder setRequestId(java.lang.String value) {
      validate(fields()[0], value);
      this.requestId = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'requestId' field has been set.
      * ID der ursprünglichen Lookup-Anfrage
      * @return True if the 'requestId' field has been set, false otherwise.
      */
    public boolean hasRequestId() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'requestId' field.
      * ID der ursprünglichen Lookup-Anfrage
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder clearRequestId() {
      requestId = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'status' field.
      * Status der Lookup-Anfrage
      * @return The value.
      */
    public de.mhus.nimbus.shared.avro.LookupStatus getStatus() {
      return status;
    }


    /**
      * Sets the value of the 'status' field.
      * Status der Lookup-Anfrage
      * @param value The value of 'status'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder setStatus(de.mhus.nimbus.shared.avro.LookupStatus value) {
      validate(fields()[1], value);
      this.status = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'status' field has been set.
      * Status der Lookup-Anfrage
      * @return True if the 'status' field has been set, false otherwise.
      */
    public boolean hasStatus() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'status' field.
      * Status der Lookup-Anfrage
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder clearStatus() {
      status = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'service' field.
      * Name des gesuchten Services
      * @return The value.
      */
    public java.lang.String getService() {
      return service;
    }


    /**
      * Sets the value of the 'service' field.
      * Name des gesuchten Services
      * @param value The value of 'service'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder setService(java.lang.String value) {
      validate(fields()[2], value);
      this.service = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'service' field has been set.
      * Name des gesuchten Services
      * @return True if the 'service' field has been set, false otherwise.
      */
    public boolean hasService() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'service' field.
      * Name des gesuchten Services
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder clearService() {
      service = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'serviceInstances' field.
      * Liste der gefundenen Service-Instanzen
      * @return The value.
      */
    public java.util.List<de.mhus.nimbus.shared.avro.ServiceInstance> getServiceInstances() {
      return serviceInstances;
    }


    /**
      * Sets the value of the 'serviceInstances' field.
      * Liste der gefundenen Service-Instanzen
      * @param value The value of 'serviceInstances'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder setServiceInstances(java.util.List<de.mhus.nimbus.shared.avro.ServiceInstance> value) {
      validate(fields()[3], value);
      this.serviceInstances = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'serviceInstances' field has been set.
      * Liste der gefundenen Service-Instanzen
      * @return True if the 'serviceInstances' field has been set, false otherwise.
      */
    public boolean hasServiceInstances() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'serviceInstances' field.
      * Liste der gefundenen Service-Instanzen
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder clearServiceInstances() {
      serviceInstances = null;
      fieldSetFlags()[3] = false;
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
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder setTimestamp(java.time.Instant value) {
      validate(fields()[4], value);
      this.timestamp = value.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'timestamp' field has been set.
      * Zeitstempel der Antwort
      * @return True if the 'timestamp' field has been set, false otherwise.
      */
    public boolean hasTimestamp() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'timestamp' field.
      * Zeitstempel der Antwort
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder clearTimestamp() {
      fieldSetFlags()[4] = false;
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
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder setErrorMessage(java.lang.String value) {
      validate(fields()[5], value);
      this.errorMessage = value;
      fieldSetFlags()[5] = true;
      return this;
    }

    /**
      * Checks whether the 'errorMessage' field has been set.
      * Fehlermeldung bei ERROR-Status
      * @return True if the 'errorMessage' field has been set, false otherwise.
      */
    public boolean hasErrorMessage() {
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'errorMessage' field.
      * Fehlermeldung bei ERROR-Status
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.LookupResponse.Builder clearErrorMessage() {
      errorMessage = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public LookupResponse build() {
      try {
        LookupResponse record = new LookupResponse();
        record.requestId = fieldSetFlags()[0] ? this.requestId : (java.lang.String) defaultValue(fields()[0]);
        record.status = fieldSetFlags()[1] ? this.status : (de.mhus.nimbus.shared.avro.LookupStatus) defaultValue(fields()[1]);
        record.service = fieldSetFlags()[2] ? this.service : (java.lang.String) defaultValue(fields()[2]);
        record.serviceInstances = fieldSetFlags()[3] ? this.serviceInstances : (java.util.List<de.mhus.nimbus.shared.avro.ServiceInstance>) defaultValue(fields()[3]);
        record.timestamp = fieldSetFlags()[4] ? this.timestamp : (java.time.Instant) defaultValue(fields()[4]);
        record.errorMessage = fieldSetFlags()[5] ? this.errorMessage : (java.lang.String) defaultValue(fields()[5]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<LookupResponse>
    WRITER$ = (org.apache.avro.io.DatumWriter<LookupResponse>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<LookupResponse>
    READER$ = (org.apache.avro.io.DatumReader<LookupResponse>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}










