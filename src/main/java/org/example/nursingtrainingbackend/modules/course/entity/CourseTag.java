package org.example.nursingtrainingbackend.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_tag")
public class CourseTag {

    @TableId
    private Long courseId;

    private Long tagId;

    private LocalDateTime createdAt;
}