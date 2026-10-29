package com.example.nosqllab2.exception.custom;

public class InvalidShowDateException extends RuntimeException {

    public InvalidShowDateException() {
        super("Поле showDate должно иметь формат DD.MM HH:mm");
    }
}
