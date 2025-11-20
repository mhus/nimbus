package de.mhus.nimbus.generated.network;

@lombok.Data
@lombok.Builder
public class LoginErrorData extends Object {
    private boolean success;
    private double errorCode;
    private String errorMessage;
}
