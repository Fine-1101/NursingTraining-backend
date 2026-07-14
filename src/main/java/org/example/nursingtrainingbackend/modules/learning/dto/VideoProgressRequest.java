package org.example.nursingtrainingbackend.modules.learning.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

public record VideoProgressRequest(
        @NotNull(message = "currentSeconds不能为空")
        @Min(value = 0, message = "currentSeconds必须>=0")
        Integer currentSeconds,

        @NotNull(message = "durationSeconds不能为空")
        @Min(value = 1, message = "durationSeconds必须>=1")
        Integer durationSeconds,

        @NotNull(message = "ended不能为空")
        Boolean ended,

        String eventType
) {
    private static final Set<String> VALID_EVENT_TYPES = Set.of("PLAY", "AUTO", "PAUSE", "LEAVE", "ENDED");

    public boolean isVideoEnded() {
        return Boolean.TRUE.equals(ended);
    }

    public boolean isProgressInRange() {
        if (currentSeconds == null || durationSeconds == null) return false;
        return durationSeconds > 0
                && currentSeconds >= 0
                && currentSeconds <= durationSeconds + 2;
    }

    public boolean isEventValid() {
        if (currentSeconds == null || durationSeconds == null || ended == null) return false;
        if (currentSeconds < 0) return false;
        if (durationSeconds < 1) return false;
        if (currentSeconds > durationSeconds + 2) return false;
        if (eventType != null && !VALID_EVENT_TYPES.contains(eventType)) return false;
        return true;
    }
}
