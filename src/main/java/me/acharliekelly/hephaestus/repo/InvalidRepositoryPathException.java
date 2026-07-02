package me.acharliekelly.hephaestus.repo;

public class InvalidRepositoryPathException extends RuntimeException {
    public InvalidRepositoryPathException(String message) {
        super(message);
    }

    public InvalidRepositoryPathException(String message, Throwable cause) {
        super(message, cause);
    }
}

