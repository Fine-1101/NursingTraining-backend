package org.example.nursingtrainingbackend.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考核题库（目前仅支持单选题和判断题）
 */
@Data
@TableName("assessment_question")
public class AssessmentQuestion {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属课程类别ID */
    private Long categoryId;

    /** 题型：1-单选题 2-判断题 */
    private Integer questionType;

    /** 题干 */
    private String stem;

    /** 答案解析 */
    private String analysis;

    /** 难度：1-简单 2-中等 3-困难 */
    private Integer difficulty;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 创建人ID */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 软删除时间 */
    @TableLogic
    private LocalDateTime deletedAt;
}
