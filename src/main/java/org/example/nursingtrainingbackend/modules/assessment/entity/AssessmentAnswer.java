package org.example.nursingtrainingbackend.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员答题记录
 */
@Data
@TableName("assessment_answer")
public class AssessmentAnswer {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试记录ID */
    private Long attemptId;

    /** 试卷题目ID */
    private Long attemptQuestionId;

    /** 学员选择的答案 */
    private String selectedOptionKey;

    /** 是否答对：0-否 1-是 */
    private Integer isCorrect;

    /** 得分 */
    private BigDecimal score;

    /** 答题时间 */
    private LocalDateTime answeredAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
