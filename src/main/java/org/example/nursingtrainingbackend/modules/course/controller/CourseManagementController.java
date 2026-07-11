package org.example.nursingtrainingbackend.modules.course.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.course.dto.CourseStatusDTO;
import org.example.nursingtrainingbackend.modules.course.dto.ExportCourseDTO;
import org.example.nursingtrainingbackend.modules.course.dto.GetCourseDTO;
import org.example.nursingtrainingbackend.modules.course.service.CourseManagementService;
import org.example.nursingtrainingbackend.modules.course.vo.CourseDetailVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseItemVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseOverviewVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class CourseManagementController {
    @Autowired
    private CourseManagementService courseManagementService;

    @GetMapping("/courses")
    public Result<PageResult<CourseItemVO>> getCourses(@Validated GetCourseDTO dto) {
        return Result.success(courseManagementService.getCourses(dto));
    }

    @GetMapping("/courses/overview")
    public Result<CourseOverviewVO> getCourseOverview() {
        return Result.success(courseManagementService.getCourseOverview());
    }

    @GetMapping("/courses/{courseId}/preview")
    public Result<CourseDetailVO> previewCourse(@PathVariable Long courseId) {
        return Result.success(courseManagementService.getCoursePreview(courseId));
    }

    @GetMapping("/courses/export")
    public void exportCourses(@Validated ExportCourseDTO dto, HttpServletResponse response) {
        courseManagementService.exportCourses(dto, response);
    }

    @PatchMapping("/courses/{courseId}/status/s")
    public Result<CourseStatusVO> updateCourseStatus(@PathVariable Long courseId,
                                                     @Validated @RequestBody CourseStatusDTO dto) {
        return Result.success(courseManagementService.updateCourseStatus(courseId, dto));
    }

    @DeleteMapping("/courses/{courseId}")
    public Result<Void> deleteCourse(@PathVariable Long courseId) {
        courseManagementService.deleteCourse(courseId);
        return Result.success();
    }
}
