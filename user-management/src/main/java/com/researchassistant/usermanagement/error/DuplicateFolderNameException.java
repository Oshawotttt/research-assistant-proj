package com.researchassistant.usermanagement.error;

public class DuplicateFolderNameException extends RuntimeException {
    public DuplicateFolderNameException(String name) {
        super("You already have a folder named \"" + name + "\"");
    }
}
