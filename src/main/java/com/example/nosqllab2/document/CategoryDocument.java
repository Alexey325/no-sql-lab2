package com.example.nosqllab2.document;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CategoryDocument {

    @DocumentId
    private String id;

    private String name;

    private List<String> movieIds = new ArrayList<>();

    private Map<String, MovieCopy> movies = new HashMap<>();

    private int schemaVersion = SchemaVersion.CURRENT;
}
