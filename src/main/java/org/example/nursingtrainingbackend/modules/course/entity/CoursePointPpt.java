package org.example.nursingtrainingbackend.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_point_ppt")
public class CoursePointPpt {

    @TableId
    private Long coursePointId;

    private Long pptId;

    private Integer sort;

    private LocalDateTime createdAt;
}