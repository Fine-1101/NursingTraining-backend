package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考核列表项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentListItemVO {

    private Long id;
    private String title;
    private Long courseId;
    private String courseTitle;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer durationMinutes;
    private BigDecimal totalScore;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private Integer status;

    /** 参与人数（去重用户数） */
    private Long participantCount;
    /** 通过人数（去重用户数） */
    private Long passedCount;
}
