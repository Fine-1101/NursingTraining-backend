package org.example.nursingtrainingbackend.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_point_article")
public class CoursePointArticle {

    @TableId
    private Long coursePointId;

    private Long articleId;

    private Integer sort;

    private LocalDateTime createdAt;
}