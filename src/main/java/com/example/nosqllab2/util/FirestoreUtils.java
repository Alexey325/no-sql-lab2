package com.example.nosqllab2.util;

import com.google.api.core.ApiFuture;

import java.util.concurrent.ExecutionException;

public final class FirestoreUtils {

    private FirestoreUtils() {
    }

    public static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Запрос к Firestore прерван", ex);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new IllegalStateException("Ошибка Firestore: " + ex.getCause().getMessage(), ex.getCause());
        }
    }
}
