package com.researchassistant.usermanagement.error;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String email) {
        super("An account already exists for " + email);
    }
}
