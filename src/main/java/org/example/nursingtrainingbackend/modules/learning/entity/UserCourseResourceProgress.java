package org.example.nursingtrainingbackend.modules.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员课件学习进度实体
 */
@Data
@TableName("user_course_resource_progress")
public class UserCourseResourceProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学员ID */
    private Long userId;

    /** 课程ID */
    private Long courseId;

    /** 课程点ID */
    private Long coursePointId;

    /** 课件类型：1-文章，2-视频，3-PPT */
    private Integer resourceType;

    /** 课件ID */
    private Long resourceId;

    /** 状态：0-未开始，1-学习中，2-已完成 */
    private Integer status;

    /** 课件进度百分比 */
    private BigDecimal progressPercent;

    /** 最近停留位置（秒），主要用于视频续播 */
    private Integer lastPositionSeconds;

    /** 历史最远播放位置（秒），主要用于视频完成判断 */
    private Integer maxPositionSeconds;

    /** 视频总时长（秒） */
    private Integer durationSeconds;

    /** 开始学习时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
