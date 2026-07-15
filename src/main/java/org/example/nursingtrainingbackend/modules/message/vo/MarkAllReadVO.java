package org.example.nursingtrainingbackend.modules.message.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MarkAllReadVO {
    private Long updatedCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "GMT+8")
    private OffsetDateTime readAt;
}
