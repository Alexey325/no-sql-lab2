package com.example.nosqllab2.document;

public enum MovieState {
    PLANNED,
    RESCHEDULED,
    SHOWN;

    public boolean canMoveTo(MovieState next) {
        return switch (this) {
            case PLANNED, RESCHEDULED -> next == RESCHEDULED || next == SHOWN;
            case SHOWN -> false;
        };
    }
}
