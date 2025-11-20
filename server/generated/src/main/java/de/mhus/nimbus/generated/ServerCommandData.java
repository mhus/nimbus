package de.mhus.nimbus.generated;

public interface ServerCommandData {
    String getCmd();
    java.util.List<String> getArgs();
    java.lang.Boolean getOneway();
    java.util.List<SingleServerCommandData> getCmds();
    java.lang.Boolean getParallel();
}
