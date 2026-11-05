package com.example.nosqllab2.migration;

import com.example.nosqllab2.document.SchemaVersion;
import com.example.nosqllab2.util.ShowDateParser;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.example.nosqllab2.util.FirestoreUtils.await;

// строковые даты → Timestamp
@Component
public class DateToTimestampMigration implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DateToTimestampMigration.class);
    private static final DateTimeFormatter OLD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int BATCH_SIZE = 400;

    private final Firestore firestore;

    public DateToTimestampMigration(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void afterPropertiesSet() {
        int movies = migrate("movies", DateToTimestampMigration::convertMovie);
        int categories = migrate("categories", DateToTimestampMigration::convertCategory);

        log.info("Миграция дат: обновлено фильмов {}, категорий {}", movies, categories);
    }

    private int migrate(String collection, Function<DocumentSnapshot, Map<String, Object>> converter) {
        WriteBatch batch = firestore.batch();
        int inBatch = 0;
        int migrated = 0;

        for (QueryDocumentSnapshot document : await(firestore.collection(collection).get()).getDocuments()) {
            Long version = document.getLong("schemaVersion");

            if (version != null && version >= SchemaVersion.CURRENT) {
                continue;
            }

            Map<String, Object> fields = converter.apply(document);
            fields.put("schemaVersion", SchemaVersion.CURRENT);

            batch.update(document.getReference(), fields);
            inBatch++;
            migrated++;

            if (inBatch == BATCH_SIZE) {
                await(batch.commit());
                batch = firestore.batch();
                inBatch = 0;
            }
        }

        if (inBatch > 0) {
            await(batch.commit());
        }

        return migrated;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> convertMovie(DocumentSnapshot movie) {
        Map<String, Object> fields = new HashMap<>();

        putConverted(fields, "showDate", movie.get("showDate"));
        putConverted(fields, "endDate", movie.get("endDate"));

        Map<String, Object> lastStage = (Map<String, Object>) movie.get("lastStage");

        if (lastStage != null) {
            fields.put("lastStage", convertStage(lastStage));
        }

        List<Map<String, Object>> stages = (List<Map<String, Object>>) movie.get("stages");

        if (stages != null) {
            fields.put("stages", stages.stream().map(DateToTimestampMigration::convertStage).toList());
        }

        return fields;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> convertCategory(DocumentSnapshot category) {
        Map<String, Object> fields = new HashMap<>();
        Map<String, Map<String, Object>> movies = (Map<String, Map<String, Object>>) category.get("movies");

        if (movies != null) {
            movies.forEach((id, copy) -> putConverted(fields, "movies." + id + ".showDate", copy.get("showDate")));
        }

        return fields;
    }

    private static Map<String, Object> convertStage(Map<String, Object> stage) {
        Map<String, Object> converted = new HashMap<>(stage);
        putConverted(converted, "at", stage.get("at"));

        return converted;
    }

    private static void putConverted(Map<String, Object> fields, String key, Object value) {
        if (value instanceof String text) {
            fields.put(key, ShowDateParser.toStoredValue(LocalDateTime.parse(text, OLD_FORMAT)));
        }
    }
}
