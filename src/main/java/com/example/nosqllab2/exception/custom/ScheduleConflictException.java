package com.example.nosqllab2.exception.custom;

public class ScheduleConflictException extends RuntimeException {

    public ScheduleConflictException() {
        super("На указанное время уже запланирован другой фильм");
    }
}
