package com.example.nosqllab2.repository.firestore;

import com.example.nosqllab2.document.MovieState;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.UpdateBuilder;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static com.example.nosqllab2.util.FirestoreUtils.await;

@Repository
public class MovieStatsRepository {

    private final DocumentReference moviesByState;

    public MovieStatsRepository(Firestore firestore) {
        this.moviesByState = firestore.collection("stats").document("moviesByState");
    }

    public Map<MovieState, Long> read() {
        DocumentSnapshot snapshot = await(moviesByState.get());
        Map<MovieState, Long> counts = new EnumMap<>(MovieState.class);

        for (MovieState state : MovieState.values()) {
            Long count = snapshot.getLong(state.name());

            if (count == null) {
                count = 0L;
            }

            counts.put(state, count);
        }

        return counts;
    }

    public void increment(UpdateBuilder<?> writes, Map<MovieState, Long> deltas) {
        Map<String, Object> fields = new HashMap<>();

        deltas.forEach((state, delta) -> fields.put(state.name(), FieldValue.increment(delta)));

        //merge - если документа или поля нет, они будут созданы
        writes.set(moviesByState, fields, SetOptions.merge());
    }
}
