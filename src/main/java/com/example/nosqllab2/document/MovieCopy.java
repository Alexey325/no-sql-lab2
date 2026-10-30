package com.example.nosqllab2.document;

import lombok.Getter;
import lombok.Setter;

// копия содержит ровно то, что отдаёт GET /api/categories/{id}, оригинал лежит в коллекции movies
@Getter
@Setter
public class MovieCopy {

    private String title;

    private String description;

    private String showDate;

    private int duration;
}
