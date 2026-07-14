package org.example.nursingtrainingbackend.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程考核
 */
@Data
@TableName("assessment")
public class Assessment {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属课程ID */
    private Long courseId;

    /** 发布时课程类别快照 */
    private Long categoryId;

    /** 考核名称 */
    private String title;

    /** 考核说明 */
    private String description;

    /** 开考时间 */
    private LocalDateTime startAt;

    /** 最晚可开考时间，为空表示不限制 */
    private LocalDateTime endAt;

    /** 答题时长（分钟） */
    private Integer durationMinutes;

    /** 总分 */
    private BigDecimal totalScore;

    /** 及格分 */
    private BigDecimal passScore;

    /** 最多考试次数 */
    private Integer maxAttempts;

    /** 状态：0-草稿 1-已发布 2-已关闭 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishedAt;

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
