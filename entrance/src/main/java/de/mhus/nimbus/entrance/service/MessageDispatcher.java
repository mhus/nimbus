package de.mhus.nimbus.entrance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.common.client.IdentityClient;
import de.mhus.nimbus.common.client.RegistryClient;
import de.mhus.nimbus.common.client.WorldLifeClient;
import de.mhus.nimbus.common.client.WorldVoxelClient;
import de.mhus.nimbus.shared.dto.WebSocketMessage;
import de.mhus.nimbus.shared.dto.FunctionCallRequest;
import de.mhus.nimbus.shared.dto.FunctionCallResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service für die Weiterleitung von Funktionsaufrufen an die entsprechenden Client-Services
 */
@Service
@Slf4j
public class MessageDispatcher {

    private final IdentityClient identityClient;
    private final RegistryClient registryClient;
    private final WorldLifeClient worldLifeClient;
    private final WorldVoxelClient worldVoxelClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> serviceMap;

    public MessageDispatcher(IdentityClient identityClient,
                           RegistryClient registryClient,
                           WorldLifeClient worldLifeClient,
                           WorldVoxelClient worldVoxelClient,
                           ObjectMapper objectMapper) {
        this.identityClient = identityClient;
        this.registryClient = registryClient;
        this.worldLifeClient = worldLifeClient;
        this.worldVoxelClient = worldVoxelClient;
        this.objectMapper = objectMapper;

        // Service-Mapping für einfache Weiterleitung
        this.serviceMap = new HashMap<>();
        this.serviceMap.put("identity", identityClient);
        this.serviceMap.put("registry", registryClient);
        this.serviceMap.put("world-life", worldLifeClient);
        this.serviceMap.put("world-voxel", worldVoxelClient);
    }

    /**
     * Dispatcht eine Nachricht an den entsprechenden Service
     */
    public void dispatch(WebSocketSession session, WebSocketMessage message) {
        try {
            if ("function_call".equals(message.getType())) {
                handleFunctionCall(session, message);
            } else {
                sendErrorResponse(session, message.getRequestId(), "Unbekannter Nachrichtentyp: " + message.getType());
            }
        } catch (Exception e) {
            log.error("Fehler beim Dispatching der Nachricht: {}", e.getMessage(), e);
            sendErrorResponse(session, message.getRequestId(), "Interner Serverfehler");
        }
    }

    private void handleFunctionCall(WebSocketSession session, WebSocketMessage message) {
        try {
            FunctionCallRequest request = objectMapper.convertValue(message.getData(), FunctionCallRequest.class);

            String serviceName = request.getService();
            String methodName = request.getMethod();
            Object[] parameters = request.getParameters() != null ? request.getParameters() : new Object[0];

            Object service = serviceMap.get(serviceName);
            if (service == null) {
                sendErrorResponse(session, message.getRequestId(), "Service nicht gefunden: " + serviceName);
                return;
            }

            // Asynchrone Ausführung des Funktionsaufrufs
            CompletableFuture.supplyAsync(() -> {
                try {
                    return invokeMethod(service, methodName, parameters);
                } catch (Exception e) {
                    log.error("Fehler beim Ausführen der Methode {}.{}: {}", serviceName, methodName, e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }).thenAccept(result -> {
                try {
                    FunctionCallResponse response = FunctionCallResponse.builder()
                        .success(true)
                        .result(result)
                        .build();

                    WebSocketMessage responseMessage = WebSocketMessage.builder()
                        .type("function_response")
                        .requestId(message.getRequestId())
                        .data(response)
                        .build();

                    sendMessage(session, responseMessage);
                } catch (Exception e) {
                    log.error("Fehler beim Senden der Antwort: {}", e.getMessage(), e);
                    sendErrorResponse(session, message.getRequestId(), "Fehler beim Senden der Antwort");
                }
            }).exceptionally(throwable -> {
                log.error("Fehler beim Ausführen der Funktion: {}", throwable.getMessage(), throwable);
                sendErrorResponse(session, message.getRequestId(), "Fehler beim Ausführen der Funktion: " + throwable.getMessage());
                return null;
            });

        } catch (Exception e) {
            log.error("Fehler beim Verarbeiten des Funktionsaufrufs: {}", e.getMessage(), e);
            sendErrorResponse(session, message.getRequestId(), "Fehler beim Verarbeiten des Funktionsaufrufs");
        }
    }

    private Object invokeMethod(Object service, String methodName, Object[] parameters) throws Exception {
        // Vereinfachte Methodenauflösung - in einer Produktionsumgebung sollte dies robuster sein
        Class<?> serviceClass = service.getClass();
        Method[] methods = serviceClass.getMethods();

        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameters.length) {
                return method.invoke(service, parameters);
            }
        }

        throw new NoSuchMethodException("Methode " + methodName + " mit " + parameters.length + " Parametern nicht gefunden in " + serviceClass.getSimpleName());
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Fehler beim Senden der Nachricht: {}", e.getMessage(), e);
        }
    }

    private void sendErrorResponse(WebSocketSession session, String requestId, String errorMessage) {
        try {
            FunctionCallResponse errorResponse = FunctionCallResponse.builder()
                .success(false)
                .error(errorMessage)
                .build();

            WebSocketMessage responseMessage = WebSocketMessage.builder()
                .type("function_response")
                .requestId(requestId)
                .data(errorResponse)
                .build();

            sendMessage(session, responseMessage);
        } catch (Exception e) {
            log.error("Fehler beim Senden der Fehlernachricht: {}", e.getMessage(), e);
        }
    }
}
