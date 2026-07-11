package org.example.nursingtrainingbackend.modules.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_ppt")
public class CoursePpt {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pptId;

    private Long courseId;

    private LocalDateTime createdAt;
}
