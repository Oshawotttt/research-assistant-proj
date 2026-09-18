package com.researchassistant.usermanagement.error;

/**
 * Thrown for BOTH an unknown email and a wrong password, with the same message,
 * so the endpoint cannot be used to enumerate which addresses are registered.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
