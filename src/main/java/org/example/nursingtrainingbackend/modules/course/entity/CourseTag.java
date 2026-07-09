package org.example.nursingtrainingbackend.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_tag")
public class CourseTag {

    private Long courseId;

    private Long tagId;

    private LocalDateTime createdAt;
}