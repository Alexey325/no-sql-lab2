package com.example.nosqllab2.document;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MovieDocument {

    @DocumentId
    private String id;

    private String title;

    private String description;

    private String showDate;

    private int duration;

    private String categoryId;

    private String categoryName;

    private MovieState state;

    private List<MovieStage> stages = new ArrayList<>();

    // плоская копия имён этапов, по массиву строк работает array-contains
    private List<String> stageNames = new ArrayList<>();
}
