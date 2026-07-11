package org.example.nursingtrainingbackend.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.entity.CourseChapter;
import org.example.nursingtrainingbackend.modules.course.entity.CoursePoint;
import org.example.nursingtrainingbackend.modules.course.vo.CourseExportRowVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseItemVO;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    @Select("SELECT IFNULL(MAX(sort), 0) FROM course_chapter")
    Integer selectMax();

    IPage<CourseItemVO> selectCoursePage(Page<CourseItemVO> page,
                                         @Param("keyword") String keyword,
                                         @Param("categoryId") Long categoryId,
                                         @Param("status") Integer status);

    List<CourseExportRowVO> selectCourseExport(@Param("keyword") String keyword,
                                               @Param("categoryId") Long categoryId,
                                               @Param("status") Integer status);

    @Select("SELECT * FROM course_chapter WHERE course_id = #{id} ORDER BY sort ASC")
    CourseChapter selectChapterByCourseId(Long id);

    @Select("SELECT * FROM course_point WHERE course_id = #{id} ORDER BY sort ASC")
    CoursePoint selectPointByCourseId(Long id);
}
