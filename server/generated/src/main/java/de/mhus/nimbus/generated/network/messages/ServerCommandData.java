package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class ServerCommandData extends Object {
    private String cmd;
    private java.util.List<String> args;
    private java.lang.Boolean oneway;
    private java.util.List<SingleServerCommandData> cmds;
    private java.lang.Boolean parallel;
}
