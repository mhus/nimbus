# Client Common Module

Das `client-common` Modul stellt eine Bean zur Verfügung, die WebSocket-Verbindungen zum Nimbus Entrance Server aufbauen kann. Es ist als eigenständige Bibliothek konzipiert, die ohne Spring-Framework verwendet werden kann.

## Features

- **WebSocket-Client**: Verbindung zum Nimbus Entrance Server über WebSocket
- **Authentifizierung**: Unterstützung für Benutzerauthentifizierung
- **Funktionsaufrufe**: Aufruf von Server-Funktionen über WebSocket
- **Message Handling**: Flexibles System für eingehende Nachrichten
- **Builder Pattern**: Einfache Konfiguration über Builder-Klasse
- **Keine Spring-Abhängigkeit**: Kann in jeder Java-Anwendung verwendet werden

## Abhängigkeiten

Das Modul verwendet:
- **Shared Module**: Für DTOs (WebSocketMessage, etc.)
- **Java-WebSocket**: Für WebSocket-Client-Funktionalität
- **Jackson**: Für JSON-Serialisierung
- **SLF4J**: Für Logging
- **Lombok**: Für Code-Generierung

## Verwendung

### Einfache Verwendung

```java
NimbusClientService clientService = new NimbusClientService();

// Handler für Nachrichten registrieren
clientService.registerMessageHandler("notification", message -> {
    System.out.println("Benachrichtigung: " + message.getData());
});

// Mit Server verbinden
clientService.connect("ws://localhost:8080/nimbus")
    .thenCompose(v -> clientService.authenticate("username", "password", "MyClient/1.0"))
    .thenCompose(authResponse -> clientService.callFunction("getUserProfile", null))
    .thenAccept(response -> System.out.println("Antwort: " + response))
    .whenComplete((result, throwable) -> clientService.disconnect());
```

### Mit Builder Pattern

```java
NimbusClientBuilder.create()
    .serverUrl("ws://localhost:8080/nimbus")
    .defaultMessageHandler(message -> System.out.println("Nachricht: " + message))
    .autoReconnect(true)
    .buildAndConnect()
    .thenCompose(client -> client.authenticate("user", "pass", "Client/1.0"))
    .thenAccept(client -> {
        client.callFunction("someFunction", parameters);
    });
```

## Architektur

### NimbusClientService
Die Haupt-Bean, die alle WebSocket-Funktionalitäten bereitstellt:
- Verbindungsmanagement
- Authentifizierung
- Funktionsaufrufe
- Message Handling

### NimbusWebSocketClient
Erweitert den Java-WebSocket Client und bietet:
- Automatisches Request-Response-Mapping
- Handler-System für verschiedene Nachrichtentypen
- JSON-Serialisierung

### NimbusClientBuilder
Builder-Klasse für einfache Konfiguration und Setup.

## DTOs

Das Modul verwendet die DTOs aus dem `shared` Modul:
- `WebSocketMessage`: Basis-Nachrichtenformat
- Authentifizierungs- und Funktionsaufruf-DTOs werden zur Laufzeit erstellt

## Logging

Das Modul verwendet SLF4J für Logging. Beispiel für Logback-Konfiguration:

```xml
<logger name="de.mhus.nimbus.client.common" level="DEBUG"/>
```

## Thread-Sicherheit

Der NimbusClientService ist thread-sicher und kann von mehreren Threads verwendet werden.
