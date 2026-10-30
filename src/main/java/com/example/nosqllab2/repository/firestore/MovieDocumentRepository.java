package com.example.nosqllab2.repository.firestore;

import com.example.nosqllab2.document.MovieDocument;
import com.example.nosqllab2.util.ShowDateParser;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.nosqllab2.util.FirestoreUtils.await;

@Repository
public class MovieDocumentRepository {

    private final CollectionReference movies;

    public MovieDocumentRepository(Firestore firestore) {
        this.movies = firestore.collection("movies");
    }

    public MovieDocument save(MovieDocument movie) {
        DocumentReference ref;

        if (movie.getId() == null) {
            ref = movies.document();
        } else {
            ref = movies.document(movie.getId());
        }

        await(ref.set(movie));
        movie.setId(ref.getId());

        return movie;
    }

    public Optional<MovieDocument> findById(String id) {
        DocumentSnapshot snapshot = await(movies.document(id).get());

        if (!snapshot.exists()) {
            return Optional.empty();
        }

        return Optional.ofNullable(snapshot.toObject(MovieDocument.class));
    }


    public Optional<MovieDocument> findWithCategoryById(String id) {
        return findById(id);
    }

    public List<MovieDocument> findAll() {
        return await(movies.orderBy("showDate").get()).toObjects(MovieDocument.class);
    }

    public List<MovieDocument> findAllByShowDateBetween(LocalDateTime start, LocalDateTime end) {
        Query query = movies
                .whereGreaterThanOrEqualTo("showDate", ShowDateParser.toStoredValue(start))
                .whereLessThanOrEqualTo("showDate", ShowDateParser.toStoredValue(end));

        return await(query.get()).toObjects(MovieDocument.class);
    }

    public void delete(MovieDocument movie) {
        await(movies.document(movie.getId()).delete());
    }

    // запись в пакет: id выдаётся сразу, а в базу документ попадёт вместе с остальными записями пакета
    public void save(WriteBatch batch, MovieDocument movie) {
        DocumentReference ref;

        if (movie.getId() == null) {
            ref = movies.document();
        } else {
            ref = movies.document(movie.getId());
        }

        movie.setId(ref.getId());
        batch.set(ref, movie);
    }

    public void delete(WriteBatch batch, MovieDocument movie) {
        batch.delete(movies.document(movie.getId()));
    }
}
