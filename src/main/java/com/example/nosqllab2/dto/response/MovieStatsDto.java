package com.example.nosqllab2.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MovieStatsDto(
        @JsonProperty("PLANNED") long planned,
        @JsonProperty("RESCHEDULED") long rescheduled,
        @JsonProperty("SHOWN") long shown
) {
}
