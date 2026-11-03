package com.example.nosqllab2.exception.custom;

import com.example.nosqllab2.document.MovieState;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(MovieState from, MovieState to) {
        super("Фильм нельзя перевести из состояния " + from + " в " + to);
    }
}
