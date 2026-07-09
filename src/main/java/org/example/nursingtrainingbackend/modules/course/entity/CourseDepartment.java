package org.example.nursingtrainingbackend.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_department")
public class CourseDepartment {

    private Long departmentId;

    private Long courseId;

    private Integer required;

    private LocalDateTime createdAt;
}