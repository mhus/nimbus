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

/** Information about an Access Control Entity */
@org.apache.avro.specific.AvroGenerated
public class AceInfo extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 6777840547812908868L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"AceInfo\",\"namespace\":\"de.mhus.nimbus.shared.avro\",\"doc\":\"Information about an Access Control Entity\",\"fields\":[{\"name\":\"aceId\",\"type\":\"long\",\"doc\":\"The ID of the ACE\"},{\"name\":\"rule\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"The access control rule\"},{\"name\":\"orderValue\",\"type\":\"int\",\"doc\":\"The order value of the ACE\"},{\"name\":\"description\",\"type\":[\"null\",{\"type\":\"string\",\"avro.java.string\":\"String\"}],\"doc\":\"Optional description of the ACE\",\"default\":null},{\"name\":\"active\",\"type\":\"boolean\",\"doc\":\"Whether the ACE is active\"},{\"name\":\"createdAt\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"},\"doc\":\"Timestamp when the ACE was created\"},{\"name\":\"updatedAt\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"},\"doc\":\"Timestamp when the ACE was last updated\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();
  static {
    MODEL$.addLogicalTypeConversion(new org.apache.avro.data.TimeConversions.TimestampMillisConversion());
  }

  private static final BinaryMessageEncoder<AceInfo> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<AceInfo> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<AceInfo> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<AceInfo> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<AceInfo> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this AceInfo to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a AceInfo from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a AceInfo instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static AceInfo fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  /** The ID of the ACE */
  private long aceId;
  /** The access control rule */
  private java.lang.String rule;
  /** The order value of the ACE */
  private int orderValue;
  /** Optional description of the ACE */
  private java.lang.String description;
  /** Whether the ACE is active */
  private boolean active;
  /** Timestamp when the ACE was created */
  private java.time.Instant createdAt;
  /** Timestamp when the ACE was last updated */
  private java.time.Instant updatedAt;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public AceInfo() {}

  /**
   * All-args constructor.
   * @param aceId The ID of the ACE
   * @param rule The access control rule
   * @param orderValue The order value of the ACE
   * @param description Optional description of the ACE
   * @param active Whether the ACE is active
   * @param createdAt Timestamp when the ACE was created
   * @param updatedAt Timestamp when the ACE was last updated
   */
  public AceInfo(java.lang.Long aceId, java.lang.String rule, java.lang.Integer orderValue, java.lang.String description, java.lang.Boolean active, java.time.Instant createdAt, java.time.Instant updatedAt) {
    this.aceId = aceId;
    this.rule = rule;
    this.orderValue = orderValue;
    this.description = description;
    this.active = active;
    this.createdAt = createdAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    this.updatedAt = updatedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
  }

  @Override
  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  // Used by DatumWriter.  Applications should not call.
  @Override
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return aceId;
    case 1: return rule;
    case 2: return orderValue;
    case 3: return description;
    case 4: return active;
    case 5: return createdAt;
    case 6: return updatedAt;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  private static final org.apache.avro.Conversion<?>[] conversions =
      new org.apache.avro.Conversion<?>[] {
      null,
      null,
      null,
      null,
      null,
      new org.apache.avro.data.TimeConversions.TimestampMillisConversion(),
      new org.apache.avro.data.TimeConversions.TimestampMillisConversion(),
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
    case 0: aceId = (java.lang.Long)value$; break;
    case 1: rule = value$ != null ? value$.toString() : null; break;
    case 2: orderValue = (java.lang.Integer)value$; break;
    case 3: description = value$ != null ? value$.toString() : null; break;
    case 4: active = (java.lang.Boolean)value$; break;
    case 5: createdAt = (java.time.Instant)value$; break;
    case 6: updatedAt = (java.time.Instant)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'aceId' field.
   * @return The ID of the ACE
   */
  public long getAceId() {
    return aceId;
  }


  /**
   * Sets the value of the 'aceId' field.
   * The ID of the ACE
   * @param value the value to set.
   */
  public void setAceId(long value) {
    this.aceId = value;
  }

  /**
   * Gets the value of the 'rule' field.
   * @return The access control rule
   */
  public java.lang.String getRule() {
    return rule;
  }


  /**
   * Sets the value of the 'rule' field.
   * The access control rule
   * @param value the value to set.
   */
  public void setRule(java.lang.String value) {
    this.rule = value;
  }

  /**
   * Gets the value of the 'orderValue' field.
   * @return The order value of the ACE
   */
  public int getOrderValue() {
    return orderValue;
  }


  /**
   * Sets the value of the 'orderValue' field.
   * The order value of the ACE
   * @param value the value to set.
   */
  public void setOrderValue(int value) {
    this.orderValue = value;
  }

  /**
   * Gets the value of the 'description' field.
   * @return Optional description of the ACE
   */
  public java.lang.String getDescription() {
    return description;
  }


  /**
   * Sets the value of the 'description' field.
   * Optional description of the ACE
   * @param value the value to set.
   */
  public void setDescription(java.lang.String value) {
    this.description = value;
  }

  /**
   * Gets the value of the 'active' field.
   * @return Whether the ACE is active
   */
  public boolean getActive() {
    return active;
  }


  /**
   * Sets the value of the 'active' field.
   * Whether the ACE is active
   * @param value the value to set.
   */
  public void setActive(boolean value) {
    this.active = value;
  }

  /**
   * Gets the value of the 'createdAt' field.
   * @return Timestamp when the ACE was created
   */
  public java.time.Instant getCreatedAt() {
    return createdAt;
  }


  /**
   * Sets the value of the 'createdAt' field.
   * Timestamp when the ACE was created
   * @param value the value to set.
   */
  public void setCreatedAt(java.time.Instant value) {
    this.createdAt = value.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
  }

  /**
   * Gets the value of the 'updatedAt' field.
   * @return Timestamp when the ACE was last updated
   */
  public java.time.Instant getUpdatedAt() {
    return updatedAt;
  }


  /**
   * Sets the value of the 'updatedAt' field.
   * Timestamp when the ACE was last updated
   * @param value the value to set.
   */
  public void setUpdatedAt(java.time.Instant value) {
    this.updatedAt = value.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
  }

  /**
   * Creates a new AceInfo RecordBuilder.
   * @return A new AceInfo RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.AceInfo.Builder newBuilder() {
    return new de.mhus.nimbus.shared.avro.AceInfo.Builder();
  }

  /**
   * Creates a new AceInfo RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new AceInfo RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.AceInfo.Builder newBuilder(de.mhus.nimbus.shared.avro.AceInfo.Builder other) {
    if (other == null) {
      return new de.mhus.nimbus.shared.avro.AceInfo.Builder();
    } else {
      return new de.mhus.nimbus.shared.avro.AceInfo.Builder(other);
    }
  }

  /**
   * Creates a new AceInfo RecordBuilder by copying an existing AceInfo instance.
   * @param other The existing instance to copy.
   * @return A new AceInfo RecordBuilder
   */
  public static de.mhus.nimbus.shared.avro.AceInfo.Builder newBuilder(de.mhus.nimbus.shared.avro.AceInfo other) {
    if (other == null) {
      return new de.mhus.nimbus.shared.avro.AceInfo.Builder();
    } else {
      return new de.mhus.nimbus.shared.avro.AceInfo.Builder(other);
    }
  }

  /**
   * RecordBuilder for AceInfo instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<AceInfo>
    implements org.apache.avro.data.RecordBuilder<AceInfo> {

    /** The ID of the ACE */
    private long aceId;
    /** The access control rule */
    private java.lang.String rule;
    /** The order value of the ACE */
    private int orderValue;
    /** Optional description of the ACE */
    private java.lang.String description;
    /** Whether the ACE is active */
    private boolean active;
    /** Timestamp when the ACE was created */
    private java.time.Instant createdAt;
    /** Timestamp when the ACE was last updated */
    private java.time.Instant updatedAt;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(de.mhus.nimbus.shared.avro.AceInfo.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.aceId)) {
        this.aceId = data().deepCopy(fields()[0].schema(), other.aceId);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.rule)) {
        this.rule = data().deepCopy(fields()[1].schema(), other.rule);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.orderValue)) {
        this.orderValue = data().deepCopy(fields()[2].schema(), other.orderValue);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
      if (isValidValue(fields()[3], other.description)) {
        this.description = data().deepCopy(fields()[3].schema(), other.description);
        fieldSetFlags()[3] = other.fieldSetFlags()[3];
      }
      if (isValidValue(fields()[4], other.active)) {
        this.active = data().deepCopy(fields()[4].schema(), other.active);
        fieldSetFlags()[4] = other.fieldSetFlags()[4];
      }
      if (isValidValue(fields()[5], other.createdAt)) {
        this.createdAt = data().deepCopy(fields()[5].schema(), other.createdAt);
        fieldSetFlags()[5] = other.fieldSetFlags()[5];
      }
      if (isValidValue(fields()[6], other.updatedAt)) {
        this.updatedAt = data().deepCopy(fields()[6].schema(), other.updatedAt);
        fieldSetFlags()[6] = other.fieldSetFlags()[6];
      }
    }

    /**
     * Creates a Builder by copying an existing AceInfo instance
     * @param other The existing instance to copy.
     */
    private Builder(de.mhus.nimbus.shared.avro.AceInfo other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.aceId)) {
        this.aceId = data().deepCopy(fields()[0].schema(), other.aceId);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.rule)) {
        this.rule = data().deepCopy(fields()[1].schema(), other.rule);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.orderValue)) {
        this.orderValue = data().deepCopy(fields()[2].schema(), other.orderValue);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.description)) {
        this.description = data().deepCopy(fields()[3].schema(), other.description);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.active)) {
        this.active = data().deepCopy(fields()[4].schema(), other.active);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.createdAt)) {
        this.createdAt = data().deepCopy(fields()[5].schema(), other.createdAt);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.updatedAt)) {
        this.updatedAt = data().deepCopy(fields()[6].schema(), other.updatedAt);
        fieldSetFlags()[6] = true;
      }
    }

    /**
      * Gets the value of the 'aceId' field.
      * The ID of the ACE
      * @return The value.
      */
    public long getAceId() {
      return aceId;
    }


    /**
      * Sets the value of the 'aceId' field.
      * The ID of the ACE
      * @param value The value of 'aceId'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder setAceId(long value) {
      validate(fields()[0], value);
      this.aceId = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'aceId' field has been set.
      * The ID of the ACE
      * @return True if the 'aceId' field has been set, false otherwise.
      */
    public boolean hasAceId() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'aceId' field.
      * The ID of the ACE
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder clearAceId() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'rule' field.
      * The access control rule
      * @return The value.
      */
    public java.lang.String getRule() {
      return rule;
    }


    /**
      * Sets the value of the 'rule' field.
      * The access control rule
      * @param value The value of 'rule'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder setRule(java.lang.String value) {
      validate(fields()[1], value);
      this.rule = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'rule' field has been set.
      * The access control rule
      * @return True if the 'rule' field has been set, false otherwise.
      */
    public boolean hasRule() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'rule' field.
      * The access control rule
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder clearRule() {
      rule = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'orderValue' field.
      * The order value of the ACE
      * @return The value.
      */
    public int getOrderValue() {
      return orderValue;
    }


    /**
      * Sets the value of the 'orderValue' field.
      * The order value of the ACE
      * @param value The value of 'orderValue'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder setOrderValue(int value) {
      validate(fields()[2], value);
      this.orderValue = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'orderValue' field has been set.
      * The order value of the ACE
      * @return True if the 'orderValue' field has been set, false otherwise.
      */
    public boolean hasOrderValue() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'orderValue' field.
      * The order value of the ACE
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder clearOrderValue() {
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'description' field.
      * Optional description of the ACE
      * @return The value.
      */
    public java.lang.String getDescription() {
      return description;
    }


    /**
      * Sets the value of the 'description' field.
      * Optional description of the ACE
      * @param value The value of 'description'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder setDescription(java.lang.String value) {
      validate(fields()[3], value);
      this.description = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'description' field has been set.
      * Optional description of the ACE
      * @return True if the 'description' field has been set, false otherwise.
      */
    public boolean hasDescription() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'description' field.
      * Optional description of the ACE
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder clearDescription() {
      description = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'active' field.
      * Whether the ACE is active
      * @return The value.
      */
    public boolean getActive() {
      return active;
    }


    /**
      * Sets the value of the 'active' field.
      * Whether the ACE is active
      * @param value The value of 'active'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder setActive(boolean value) {
      validate(fields()[4], value);
      this.active = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'active' field has been set.
      * Whether the ACE is active
      * @return True if the 'active' field has been set, false otherwise.
      */
    public boolean hasActive() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'active' field.
      * Whether the ACE is active
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder clearActive() {
      fieldSetFlags()[4] = false;
      return this;
    }

    /**
      * Gets the value of the 'createdAt' field.
      * Timestamp when the ACE was created
      * @return The value.
      */
    public java.time.Instant getCreatedAt() {
      return createdAt;
    }


    /**
      * Sets the value of the 'createdAt' field.
      * Timestamp when the ACE was created
      * @param value The value of 'createdAt'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder setCreatedAt(java.time.Instant value) {
      validate(fields()[5], value);
      this.createdAt = value.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
      fieldSetFlags()[5] = true;
      return this;
    }

    /**
      * Checks whether the 'createdAt' field has been set.
      * Timestamp when the ACE was created
      * @return True if the 'createdAt' field has been set, false otherwise.
      */
    public boolean hasCreatedAt() {
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'createdAt' field.
      * Timestamp when the ACE was created
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder clearCreatedAt() {
      fieldSetFlags()[5] = false;
      return this;
    }

    /**
      * Gets the value of the 'updatedAt' field.
      * Timestamp when the ACE was last updated
      * @return The value.
      */
    public java.time.Instant getUpdatedAt() {
      return updatedAt;
    }


    /**
      * Sets the value of the 'updatedAt' field.
      * Timestamp when the ACE was last updated
      * @param value The value of 'updatedAt'.
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder setUpdatedAt(java.time.Instant value) {
      validate(fields()[6], value);
      this.updatedAt = value.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
      fieldSetFlags()[6] = true;
      return this;
    }

    /**
      * Checks whether the 'updatedAt' field has been set.
      * Timestamp when the ACE was last updated
      * @return True if the 'updatedAt' field has been set, false otherwise.
      */
    public boolean hasUpdatedAt() {
      return fieldSetFlags()[6];
    }


    /**
      * Clears the value of the 'updatedAt' field.
      * Timestamp when the ACE was last updated
      * @return This builder.
      */
    public de.mhus.nimbus.shared.avro.AceInfo.Builder clearUpdatedAt() {
      fieldSetFlags()[6] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AceInfo build() {
      try {
        AceInfo record = new AceInfo();
        record.aceId = fieldSetFlags()[0] ? this.aceId : (java.lang.Long) defaultValue(fields()[0]);
        record.rule = fieldSetFlags()[1] ? this.rule : (java.lang.String) defaultValue(fields()[1]);
        record.orderValue = fieldSetFlags()[2] ? this.orderValue : (java.lang.Integer) defaultValue(fields()[2]);
        record.description = fieldSetFlags()[3] ? this.description : (java.lang.String) defaultValue(fields()[3]);
        record.active = fieldSetFlags()[4] ? this.active : (java.lang.Boolean) defaultValue(fields()[4]);
        record.createdAt = fieldSetFlags()[5] ? this.createdAt : (java.time.Instant) defaultValue(fields()[5]);
        record.updatedAt = fieldSetFlags()[6] ? this.updatedAt : (java.time.Instant) defaultValue(fields()[6]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<AceInfo>
    WRITER$ = (org.apache.avro.io.DatumWriter<AceInfo>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<AceInfo>
    READER$ = (org.apache.avro.io.DatumReader<AceInfo>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}










