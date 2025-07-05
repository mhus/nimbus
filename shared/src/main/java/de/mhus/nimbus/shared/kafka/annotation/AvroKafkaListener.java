package de.mhus.nimbus.shared.kafka.annotation;

import org.springframework.kafka.annotation.KafkaListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation für Avro-Kafka-Listener mit automatischer Serialisierung/Deserialisierung
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@KafkaListener
public @interface AvroKafkaListener {

    /**
     * Kafka-Topics, die überwacht werden sollen
     */
    String[] topics();

    /**
     * Consumer-Gruppe ID
     */
    String groupId() default "";

    /**
     * Response-Topic für Antworten (optional)
     */
    String responseTopic() default "";

    /**
     * Container-Factory für Kafka-Listener
     */
    String containerFactory() default "kafkaListenerContainerFactory";
}
