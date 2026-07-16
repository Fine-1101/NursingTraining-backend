package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ParticipantItemVO {

    private Long userId;
    private String username;
    private String realName;
    private Long departmentId;
    private String departmentName;
    private String participationStatus;
    private Integer attemptCount;
    private BigDecimal bestScore;
    private BigDecimal latestScore;
    private Boolean passed;
    private Long latestAttemptId;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Boolean reminded;
    private LocalDateTime lastRemindedAt;
}
