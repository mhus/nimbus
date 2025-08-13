package de.mhus.nimbus.world.bridge.command;

public interface WebSocketCommand {

    /**
     * Gibt Informationen über das Kommando zurück
     * @return WebSocketCommandInfo mit Service-Name, Kommando-Name und Beschreibung
     */
    WebSocketCommandInfo info();

    /**
     * Führt das Kommando aus
     * @param request ExecuteRequest mit Session-Informationen und Kommando-Daten
     * @return ExecuteResponse mit dem Ergebnis der Ausführung
     */
    ExecuteResponse execute(ExecuteRequest request);
}
