package com.example.nosqllab2.document;

import com.google.cloud.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieCopy {

    private String title;

    private String description;

    private Timestamp showDate;

    private int duration;
}
