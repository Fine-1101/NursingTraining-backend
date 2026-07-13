package org.example.nursingtrainingbackend.modules.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProgressResetVO {

    private Long studentId;
    private Long courseId;
    private String learningStatus;
    private BigDecimal progressPercent;
    private Long lastPointId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
