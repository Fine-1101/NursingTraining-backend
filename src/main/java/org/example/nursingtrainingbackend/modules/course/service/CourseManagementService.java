package org.example.nursingtrainingbackend.modules.course.service;

import jakarta.servlet.http.HttpServletResponse;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.course.dto.ExportCourseDTO;
import org.example.nursingtrainingbackend.modules.course.dto.GetCourseDTO;
import org.example.nursingtrainingbackend.modules.course.dto.CourseStatusDTO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseDetailVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseItemVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseOverviewVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseStatusVO;


public interface CourseManagementService {
    PageResult<CourseItemVO> getCourses(GetCourseDTO dto);

    CourseOverviewVO getCourseOverview();

    CourseDetailVO getCoursePreview(Long courseId);

    void exportCourses(ExportCourseDTO dto, HttpServletResponse response);

    CourseStatusVO updateCourseStatus(Long courseId, CourseStatusDTO dto);

    void deleteCourse(Long courseId);
}
