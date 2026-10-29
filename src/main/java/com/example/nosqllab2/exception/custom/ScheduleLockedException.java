package com.example.nosqllab2.exception.custom;

public class ScheduleLockedException extends RuntimeException {

    public ScheduleLockedException() {
        super("Расписание сейчас изменяется другим запросом");
    }
}
