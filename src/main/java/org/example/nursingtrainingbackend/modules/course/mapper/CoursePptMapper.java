package org.example.nursingtrainingbackend.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.nursingtrainingbackend.modules.course.entity.CoursePpt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CoursePptMapper extends BaseMapper<CoursePpt> {

    @Select({
            "SELECT ppt_id, COUNT(DISTINCT course_id) cnt FROM (",
            "  SELECT ppt_id, course_id FROM course_ppt",
            "  UNION",
            "  SELECT cpp.ppt_id, cp.course_id",
            "    FROM course_point_ppt cpp",
            "    INNER JOIN course_point cp ON cp.id = cpp.course_point_id",
            "    WHERE cp.deleted_at IS NULL",
            ") combined",
            "GROUP BY ppt_id"
    })
    List<Map<String, Object>> selectCourseCountGroupByPptId();

    @Insert({
            "<script>",
            "INSERT IGNORE INTO course_ppt (ppt_id, course_id, created_at) VALUES ",
            "<foreach collection='courseIds' item='cid' separator=','>",
            "(#{pptId}, #{cid}, #{createdAt})",
            "</foreach>",
            "</script>"
    })
    int batchInsertOrIgnore(@Param("pptId") Long pptId,
                             @Param("courseIds") List<Long> courseIds,
                             @Param("createdAt") LocalDateTime createdAt);

    @Delete({
            "<script>",
            "DELETE FROM course_ppt WHERE ppt_id = #{pptId} AND course_id IN ",
            "<foreach collection='courseIds' item='cid' open='(' separator=',' close=')'>",
            "#{cid}",
            "</foreach>",
            "</script>"
    })
    int batchDeleteByPptAndCourses(@Param("pptId") Long pptId,
                                   @Param("courseIds") List<Long> courseIds);

    @Select({
            "SELECT COUNT(DISTINCT course_id) FROM (",
            "  SELECT course_id FROM course_ppt WHERE ppt_id = #{pptId}",
            "  UNION",
            "  SELECT cp.course_id",
            "    FROM course_point_ppt cpp",
            "    INNER JOIN course_point cp ON cp.id = cpp.course_point_id",
            "    WHERE cpp.ppt_id = #{pptId} AND cp.deleted_at IS NULL",
            ") combined"
    })
    long countByPptId(@Param("pptId") Long pptId);
}
