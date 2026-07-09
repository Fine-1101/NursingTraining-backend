package org.example.nursingtrainingbackend.modules.tag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_tag")
public class CourseTag {

    @TableId(type = IdType.NONE)
    private Long courseId;

    private Long tagId;
}
