package com.example.nosqllab2.exception.custom;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Неверный логин или пароль");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
