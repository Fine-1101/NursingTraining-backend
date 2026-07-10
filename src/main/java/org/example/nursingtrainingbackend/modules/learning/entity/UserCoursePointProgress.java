package org.example.nursingtrainingbackend.modules.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学员课程点学习进度实体
 */
@Data
@TableName("user_course_point_progress")
public class UserCoursePointProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学员ID */
    private Long userId;

    /** 课程ID */
    private Long courseId;

    /** 课程点ID */
    private Long coursePointId;

    /** 是否必修：0-选修，1-必修 */
    private Integer required;

    /** 状态：0-未开始，1-学习中，2-已完成 */
    private Integer status;

    /** 开始学习时间 */
    private LocalDateTime startedAt;

    /** 完成课程点时间 */
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
