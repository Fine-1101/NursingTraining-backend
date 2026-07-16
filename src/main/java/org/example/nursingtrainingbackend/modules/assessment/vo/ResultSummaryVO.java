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

    /** 应参加但没有任何考试记录的人数 */
    private Long notParticipatedCount;
    /** 当前存在答题中记录的人数 */
    private Long inProgressCount;
}
