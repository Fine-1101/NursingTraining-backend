package org.example.nursingtrainingbackend.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员课程进度实体
 */
@Data
@TableName("user_course_progress")
public class UserCourseProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学员ID */
    private Long userId;

    /** 课程ID */
    private Long courseId;

    /** 进度百分比 */
    private BigDecimal progressPercent;

    /** 状态：0-未开始 1-学习中 2-已完成 */
    private Integer status;

    /** 最近学习课程点 */
    private Long lastPointId;

    /** 开始学习时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
