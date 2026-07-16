package org.example.nursingtrainingbackend.modules.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_student_message")
public class CourseStudentMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receiverId;

    private Long senderId;

    private Long courseId;

    private String courseTitle;

    /** 考核ID（考核提醒时使用） */
    private Long assessmentId;

    /** 批量发送批次号 */
    private String batchId;

    /** 消息类型：GENERAL / ASSESSMENT_REMINDER */
    private String messageType;

    private String content;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    @TableLogic
    private LocalDateTime deletedAt;


}
