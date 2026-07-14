package org.example.nursingtrainingbackend.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.nursingtrainingbackend.modules.course.entity.CourseChapter;

@Mapper
public interface CourseChapterMapper extends BaseMapper<CourseChapter> {
    @Select("select max(sort) from course_chapter")
    Integer selectMax();
}
