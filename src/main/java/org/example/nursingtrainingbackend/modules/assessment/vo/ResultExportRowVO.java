package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ResultExportRowVO {

    private String username;
    private String realName;
    private String departmentName;
    private String assessmentTitle;
    private String courseTitle;
    private Integer attemptNo;
    private BigDecimal score;
    private BigDecimal totalScore;
    private BigDecimal passScore;
    private String passedText;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Long durationSeconds;
}
