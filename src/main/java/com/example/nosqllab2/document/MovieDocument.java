package com.example.nosqllab2.document;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.Setter;

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
}
