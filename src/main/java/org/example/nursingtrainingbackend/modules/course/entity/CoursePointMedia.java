package org.example.nursingtrainingbackend.modules.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_point_media")
public class CoursePointMedia {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pointId;

    private String mediaType;

    private Long mediaId;
}
