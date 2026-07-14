package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ResultItemVO {

    private Long attemptId;
    private Long assessmentId;
    private String assessmentTitle;
    private Long courseId;
    private String courseTitle;
    private Long userId;
    private String username;
    private String realName;
    private Long departmentId;
    private String departmentName;
    private Integer attemptNo;
    private BigDecimal score;
    private BigDecimal totalScore;
    private BigDecimal passScore;
    private Boolean passed;
    private Integer status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Long durationSeconds;
}
