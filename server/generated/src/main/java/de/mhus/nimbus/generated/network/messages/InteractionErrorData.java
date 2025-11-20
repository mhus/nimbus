package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class InteractionErrorData extends Object {
    private boolean success;
    private double errorCode;
    private String errorMessage;
}
