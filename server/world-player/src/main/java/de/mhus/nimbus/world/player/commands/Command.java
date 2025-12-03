package de.mhus.nimbus.world.player.commands;

import de.mhus.nimbus.world.player.ws.PlayerSession;

import java.util.List;

/**
 * Interface for client commands.
 * Commands are executed by the CommandService and provide responses.
 */
public interface Command {

    /**
     * Get command name (e.g., "help", "say", "world").
     */
    String getName();

    /**
     * Execute command.
     *
     * @param session Current player session
     * @param args    Command arguments
     * @return CommandResult with return code and messages
     */
    CommandResult execute(PlayerSession session, List<String> args);

    /**
     * Get command help text.
     */
    String getHelp();

    /**
     * Command execution result.
     */
    class CommandResult {
        private final int returnCode;
        private final String message;
        private final List<String> streamMessages;

        public CommandResult(int returnCode, String message) {
            this(returnCode, message, null);
        }

        public CommandResult(int returnCode, String message, List<String> streamMessages) {
            this.returnCode = returnCode;
            this.message = message;
            this.streamMessages = streamMessages;
        }

        public int getReturnCode() {
            return returnCode;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getStreamMessages() {
            return streamMessages;
        }

        public boolean isSuccess() {
            return returnCode == 0;
        }

        // Factory methods
        public static CommandResult success(String message) {
            return new CommandResult(0, message);
        }

        public static CommandResult error(String message) {
            return new CommandResult(1, message);
        }

        public static CommandResult error(int returnCode, String message) {
            return new CommandResult(returnCode, message);
        }

        public static CommandResult withStreaming(int returnCode, String message, List<String> streamMessages) {
            return new CommandResult(returnCode, message, streamMessages);
        }
    }
}
