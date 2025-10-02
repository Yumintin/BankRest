package com.example.bankcards.exception;

public class NotCardOwnerException extends RuntimeException {
    public NotCardOwnerException(String message) {
        super(message);
    }
}