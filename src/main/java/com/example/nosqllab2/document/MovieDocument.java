package com.example.nosqllab2.document;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.Exclude;
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

    // showDate + duration
    private String endDate;

    private String categoryId;

    private String categoryName;

    private MovieStage lastStage;

    private List<MovieStage> stages = new ArrayList<>();

    @Exclude
    public MovieState getState() {
        return lastStage.getStage();
    }
}
