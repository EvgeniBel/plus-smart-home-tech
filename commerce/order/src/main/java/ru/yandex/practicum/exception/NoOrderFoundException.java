package ru.yandex.practicum.exception;

public class NoOrderFoundException extends RuntimeException {
    public NoOrderFoundException(String message) {
        super(message);
    }

    public NoOrderFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}