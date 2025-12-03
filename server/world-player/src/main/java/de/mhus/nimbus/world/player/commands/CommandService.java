package de.mhus.nimbus.world.player.commands;

import de.mhus.nimbus.world.player.ws.PlayerSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for executing client commands.
 * Manages command registry and delegates execution to command beans.
 * Commands are loaded lazily to avoid circular dependency with HelpCommand.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommandService {

    private final ApplicationContext applicationContext;
    private volatile Map<String, Command> commands;

    /**
     * Get commands lazily (on first access).
     */
    private Map<String, Command> getCommands() {
        if (commands == null) {
            synchronized (this) {
                if (commands == null) {
                    Map<String, Command> commandBeans = applicationContext.getBeansOfType(Command.class);
                    commands = new ConcurrentHashMap<>();

                    for (Command command : commandBeans.values()) {
                        commands.put(command.getName(), command);
                    }

                    log.info("Registered {} commands: {}", commands.size(), commands.keySet());
                }
            }
        }
        return commands;
    }

    /**
     * Execute command by name.
     *
     * @param session Current player session
     * @param commandName Command name
     * @param args Command arguments
     * @return CommandResult with return code and messages
     */
    public Command.CommandResult execute(PlayerSession session, String commandName, List<String> args) {
        Command command = getCommands().get(commandName);

        if (command == null) {
            log.warn("Command not found: {}", commandName);
            return Command.CommandResult.error(-1, "Command not found: " + commandName);
        }

        try {
            log.debug("Executing command: {} with args: {} for user: {}",
                    commandName, args, session.getDisplayName());

            return command.execute(session, args);

        } catch (Exception e) {
            log.error("Command execution failed: {}", commandName, e);
            return Command.CommandResult.error(-4, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Get all registered command names.
     */
    public List<String> getCommandNames() {
        return getCommands().keySet().stream().sorted().collect(Collectors.toList());
    }

    /**
     * Get command by name (for help text, etc.).
     */
    public Command getCommand(String name) {
        return getCommands().get(name);
    }
}
