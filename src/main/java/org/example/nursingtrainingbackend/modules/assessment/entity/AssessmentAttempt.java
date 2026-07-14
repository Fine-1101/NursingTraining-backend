package org.example.nursingtrainingbackend.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员考核记录
 */
@Data
@TableName("assessment_attempt")
public class AssessmentAttempt {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考核ID */
    private Long assessmentId;

    /** 学员ID */
    private Long userId;

    /** 第几次考试 */
    private Integer attemptNo;

    /** 状态：1-答题中 2-已交卷 3-已超时 */
    private Integer status;

    /** 开考时间 */
    private LocalDateTime startedAt;

    /** 本次考试服务端截止时间 */
    private LocalDateTime deadlineAt;

    /** 交卷时间 */
    private LocalDateTime submittedAt;

    /** 得分 */
    private BigDecimal score;

    /** 是否通过：0-否 1-是 */
    private Integer passed;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
