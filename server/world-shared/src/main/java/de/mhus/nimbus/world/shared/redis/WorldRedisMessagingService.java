package de.mhus.nimbus.world.shared.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import org.springframework.data.redis.connection.MessageListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorldRedisMessagingService {

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer container;
    private final Map<String, MessageListener> listeners = new ConcurrentHashMap<>();

    public void publish(String worldId, String channel, String message) {
        redisTemplate.convertAndSend(topic(worldId, channel), message);
    }

    public void subscribe(String worldId, String channel, BiConsumer<String,String> handler) {
        String t = topic(worldId, channel);
        if (listeners.containsKey(t)) return; // already subscribed
        MessageListener listener = (msg, pattern) -> {
            try {
                String body = new String(msg.getBody());
                handler.accept(t, body);
            } catch (Exception e) {
                log.warn("Failed to process redis message on {}: {}", t, e.getMessage(), e);
            }
        };
        container.addMessageListener(listener, ChannelTopic.of(t));
        listeners.put(t, listener);
    }

    public void unsubscribe(String worldId, String channel) {
        String t = topic(worldId, channel);
        MessageListener listener = listeners.remove(t);
        if (listener != null) {
            container.removeMessageListener(listener, ChannelTopic.of(t));
        }
    }

    private String topic(String worldId, String channel) {
        return "world:" + worldId + ";" + channel;
    }
}
