package com.example.nosqllab2.document;

import lombok.Getter;
import lombok.Setter;

// этап обработки фильма: в какое состояние перешёл и когда
@Getter
@Setter
public class MovieStage {

    private MovieState stage;

    private String at;
}
