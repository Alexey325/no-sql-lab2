package com.example.nosqllab2.exception.custom;

public class InvalidCursorException extends RuntimeException {

    public InvalidCursorException(String cursor) {
        super("Курсор " + cursor + " не найден, запросите страницу заново");
    }
}
