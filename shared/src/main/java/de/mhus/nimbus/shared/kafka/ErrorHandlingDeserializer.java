package de.mhus.nimbus.shared.kafka;

import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.DeserializationException;

import java.util.Map;

/**
 * Generic error handling deserializer that wraps other deserializers and provides
 * robust error handling for Kafka message deserialization.
 *
 * This deserializer catches exceptions during deserialization and logs them,
 * while allowing the consumer to continue processing other messages.
 */
public class ErrorHandlingDeserializer<T> implements Deserializer<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlingDeserializer.class);

    public static final String KEY_DELEGATE_CLASS = "spring.deserializer.key.delegate.class";
    public static final String VALUE_DELEGATE_CLASS = "spring.deserializer.value.delegate.class";
    public static final String TRUSTED_PACKAGES = "spring.json.trusted.packages";

    private Deserializer<T> delegate;
    private boolean isForKey;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        this.isForKey = isKey;

        try {
            String delegateClass = isKey
                ? (String) configs.get(KEY_DELEGATE_CLASS)
                : (String) configs.get(VALUE_DELEGATE_CLASS);

            if (delegateClass == null) {
                throw new IllegalStateException("Delegate deserializer class not configured for " +
                    (isKey ? "key" : "value") + " deserializer");
            }

            @SuppressWarnings("unchecked")
            Class<Deserializer<T>> clazz = (Class<Deserializer<T>>) Class.forName(delegateClass);
            this.delegate = clazz.getDeclaredConstructor().newInstance();
            this.delegate.configure(configs, isKey);

            LOGGER.info("Configured ErrorHandlingDeserializer with delegate: {} for {}",
                delegateClass, isKey ? "key" : "value");

        } catch (Exception e) {
            LOGGER.error("Failed to configure ErrorHandlingDeserializer: {}", e.getMessage(), e);
            throw new IllegalStateException("Could not configure delegate deserializer", e);
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            if (data == null) {
                LOGGER.debug("Received null data for topic: {}", topic);
                return null;
            }

            T result = delegate.deserialize(topic, data);
            LOGGER.trace("Successfully deserialized {} message from topic: {}",
                isForKey ? "key" : "value", topic);
            return result;

        } catch (Exception e) {
            LOGGER.error("Failed to deserialize {} from topic '{}': {} - Data length: {} bytes",
                isForKey ? "key" : "value", topic, e.getMessage(),
                data != null ? data.length : 0, e);

            // Log the raw data for debugging (only first 100 bytes to avoid log spam)
            if (data != null && LOGGER.isDebugEnabled()) {
                String dataPreview = new String(data, 0, Math.min(data.length, 100));
                LOGGER.debug("Raw data preview: {}", dataPreview);
            }

            // Create a DeserializationException that includes the original data
            throw new DeserializationException("Deserialization failed for topic: " + topic,
                data, false, e);
        }
    }

    @Override
    public void close() {
        if (delegate != null) {
            try {
                delegate.close();
                LOGGER.debug("Closed delegate deserializer: {}", delegate.getClass().getSimpleName());
            } catch (Exception e) {
                LOGGER.warn("Error closing delegate deserializer: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Get the underlying delegate deserializer
     */
    public Deserializer<T> getDelegate() {
        return delegate;
    }

    /**
     * Check if this deserializer is configured for keys
     */
    public boolean isForKey() {
        return isForKey;
    }
}
