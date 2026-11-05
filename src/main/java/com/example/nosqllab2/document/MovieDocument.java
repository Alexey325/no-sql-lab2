package com.example.nosqllab2.document;

import com.google.cloud.Timestamp;
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

    private Timestamp showDate;

    private int duration;

    // showDate + duration
    private Timestamp endDate;

    private String categoryId;

    private String categoryName;

    private MovieStage lastStage;

    private List<MovieStage> stages = new ArrayList<>();

    private int schemaVersion = SchemaVersion.CURRENT;

    @Exclude
    public MovieState getState() {
        return lastStage.getStage();
    }
}
