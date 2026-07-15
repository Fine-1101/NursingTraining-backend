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

    private String content;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    @TableLogic
    private LocalDateTime deletedAt;


}
