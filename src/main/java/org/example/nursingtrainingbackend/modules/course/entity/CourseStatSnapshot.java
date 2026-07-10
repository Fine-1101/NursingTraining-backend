package org.example.nursingtrainingbackend.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("course_stat_snapshot")
public class CourseStatSnapshot {

    @TableId
    private LocalDate statDate;

    private Long totalCourses;

    private Long draftCourses;

    private Long publishedCourses;

    private Long offlineCourses;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
