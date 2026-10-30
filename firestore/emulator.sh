#!/bin/sh

PROJECT=nosqllab2-local
EXPORT_DIR=/data/export
EMULATOR_API=http://localhost:8080/emulator/v1/projects/$PROJECT

latest_export() {
    find "$EXPORT_DIR" -name '*.overall_export_metadata' 2>/dev/null | sort | tail -n 1
}

save_data() {
    mkdir -p "$EXPORT_DIR"

    curl -s -X POST "$EMULATOR_API:export" \
        -H 'Content-Type: application/json' \
        -d "{\"database\": \"projects/$PROJECT/databases/(default)\", \"export_directory\": \"$EXPORT_DIR\"}"

    # каждая выгрузка создаёт новую папку, оставляем только последнюю
    ls -1d "$EXPORT_DIR"/firestore_export_* | sort | head -n -1 | xargs -r rm -rf
}

IMPORT=$(latest_export)

if [ -n "$IMPORT" ]; then
    gcloud emulators firestore start --host-port=0.0.0.0:8080 --project=$PROJECT --import-data="$IMPORT" &
else
    gcloud emulators firestore start --host-port=0.0.0.0:8080 --project=$PROJECT &
fi

EMULATOR_PID=$!

# docker compose stop/down присылает SIGTERM: сначала сохраняем данные, потом выключаем эмулятор
trap 'save_data; kill $EMULATOR_PID' TERM INT

wait $EMULATOR_PID
