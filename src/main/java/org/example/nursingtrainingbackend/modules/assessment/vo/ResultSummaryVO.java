package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResultSummaryVO {

    private Long assessmentId;
    private Long eligibleLearnerCount;
    private Long participantCount;
    private Long submittedCount;
    private Long passedCount;
    private Long failedCount;
    private BigDecimal passRate;
    private BigDecimal averageScore;
    private BigDecimal highestScore;
    private BigDecimal lowestScore;
}
