package org.example.nursingtrainingbackend.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 随机组卷规则
 */
@Data
@TableName("assessment_draw_rule")
public class AssessmentDrawRule {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考核ID */
    private Long assessmentId;

    /** 题型：1-单选题 2-判断题 */
    private Integer questionType;

    /** Difficulty: 1-easy, 2-medium, 3-hard; null keeps legacy rules unrestricted. */
    private Integer difficulty;

    /** 随机抽题数量 */
    private Integer questionCount;

    /** 每题分值 */
    private BigDecimal scorePerQuestion;
}
