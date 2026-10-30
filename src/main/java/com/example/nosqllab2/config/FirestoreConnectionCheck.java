package com.example.nosqllab2.config;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// бин Firestore создаётся без сетевого вызова, поэтому связь проверяем реальной записью и чтением
@Component
public class FirestoreConnectionCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FirestoreConnectionCheck.class);

    private final Firestore firestore;
    private final boolean emulator;
    private final String hostPort;

    public FirestoreConnectionCheck(
            Firestore firestore,
            @Value("${spring.cloud.gcp.firestore.emulator.enabled}") boolean emulator,
            @Value("${spring.cloud.gcp.firestore.host-port}") String hostPort
    ) {
        this.firestore = firestore;
        this.emulator = emulator;
        this.hostPort = hostPort;
    }

    @Override
    public void run(ApplicationArguments args) {
        DocumentReference ref = firestore.collection("_ping").document("startup");

        try {
            ref.set(Map.of("at", Instant.now().toString())).get(5, TimeUnit.SECONDS);
            Map<String, Object> data = ref.get().get(5, TimeUnit.SECONDS).getData();
            ref.delete().get(5, TimeUnit.SECONDS);

            log.info("Firestore доступен: project={}, {}, прочитано {}",
                    firestore.getOptions().getProjectId(), emulator ? "эмулятор " + hostPort : "облако", data);
        } catch (Exception ex) {
            log.error("Firestore недоступен: {}", ex.getMessage());
        }
    }
}
