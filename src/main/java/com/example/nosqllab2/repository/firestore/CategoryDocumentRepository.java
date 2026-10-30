package com.example.nosqllab2.repository.firestore;

import com.example.nosqllab2.document.CategoryDocument;
import com.example.nosqllab2.document.MovieDocument;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.stereotype.Repository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.example.nosqllab2.util.FirestoreUtils.await;

@Repository
public class CategoryDocumentRepository {

    private final Firestore firestore;
    private final CollectionReference categories;
    private final CollectionReference categoryNames;

    public CategoryDocumentRepository(Firestore firestore) {
        this.firestore = firestore;
        this.categories = firestore.collection("categories");
        this.categoryNames = firestore.collection("categoryNames");
    }

    public CategoryDocument save(CategoryDocument category) {
        DocumentReference ref;

        if (category.getId() == null) {
            ref = categories.document();
        } else {
            ref = categories.document(category.getId());
        }

        await(ref.set(category));
        category.setId(ref.getId());

        return category;
    }

    // уникального индекса в Firestore нет, поэтому имя занимается документом categoryNames/<имя>
    // в одном пакете с категорией: если имя уже занято, create падает и не записывается ничего
    public boolean create(CategoryDocument category) {
        DocumentReference ref;

        if (category.getId() == null) {
            ref = categories.document();
        } else {
            ref = categories.document(category.getId());
        }

        WriteBatch batch = firestore.batch();
        batch.create(categoryNames.document(nameKey(category.getName())), Map.of("categoryId", ref.getId()));
        batch.set(ref, category);

        try {
            await(batch.commit());
        } catch (IllegalStateException ex) {
            if (ex.getCause() instanceof AlreadyExistsException) {
                return false;
            }

            throw ex;
        }

        category.setId(ref.getId());

        return true;
    }

    public Optional<CategoryDocument> findById(String id) {
        DocumentSnapshot snapshot = await(categories.document(id).get());

        if (!snapshot.exists()) {
            return Optional.empty();
        }

        return Optional.ofNullable(snapshot.toObject(CategoryDocument.class));
    }

    public Optional<CategoryDocument> findWithMoviesById(String id) {
        return findById(id);
    }

    public List<CategoryDocument> findAll() {
        return await(categories.orderBy("name").get()).toObjects(CategoryDocument.class);
    }

    public Optional<CategoryDocument> findByName(String name) {
        List<CategoryDocument> found = await(categories.whereEqualTo("name", name).limit(1).get())
                .toObjects(CategoryDocument.class);

        return found.stream().findFirst();
    }

    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    // меняется одно поле map по пути movies.<id>, остальные копии в документе не перезаписываются
    public void putMovieCopy(WriteBatch batch, MovieDocument movie) {
        Map<String, Object> copy = Map.of(
                "title", movie.getTitle(),
                "description", movie.getDescription(),
                "showDate", movie.getShowDate(),
                "duration", movie.getDuration()
        );

        batch.update(
                categories.document(movie.getCategoryId()),
                FieldPath.of("movies", movie.getId()), copy,
                FieldPath.of("movieIds"), FieldValue.arrayUnion(movie.getId())
        );
    }

    public void removeMovieCopy(WriteBatch batch, MovieDocument movie) {
        batch.update(
                categories.document(movie.getCategoryId()),
                FieldPath.of("movies", movie.getId()), FieldValue.delete(),
                FieldPath.of("movieIds"), FieldValue.arrayRemove(movie.getId())
        );
    }

    // имя становится id документа, а символ / в id запрещён
    private static String nameKey(String name) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8);
    }
}
