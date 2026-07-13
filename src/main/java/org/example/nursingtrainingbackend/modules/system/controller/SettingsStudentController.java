package org.example.nursingtrainingbackend.modules.system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.system.dto.StudentQueryDTO;
import org.example.nursingtrainingbackend.modules.system.dto.StudentUpdateDTO;
import org.example.nursingtrainingbackend.modules.system.service.SettingsStudentService;
import org.example.nursingtrainingbackend.modules.system.vo.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class SettingsStudentController {

    private final SettingsStudentService settingsStudentService;

    /** 查询当前管理员信息 */
    @GetMapping("/current-user")
    public Result<CurrentUserVO> getCurrentUser() {
        return Result.success(settingsStudentService.getCurrentUser());
    }

    /** 分页查询学员列表 */
    @GetMapping("/students")
    public Result<PageResult<StudentListItemVO>> queryStudents(@Valid StudentQueryDTO query) {
        return Result.success(settingsStudentService.queryStudents(query));
    }

    /** 查询学员按科室分布 */
    @GetMapping("/students/department-distribution")
    public Result<DepartmentDistributionVO> getDepartmentDistribution(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return Result.success(settingsStudentService.getDepartmentDistribution(activeOnly));
    }

    /** 查询学员详情 */
    @GetMapping("/students/{studentId}")
    public Result<StudentDetailVO> getStudentDetail(@PathVariable Long studentId) {
        return Result.success(settingsStudentService.getStudentDetail(studentId));
    }

    /** 编辑学员信息 */
    @PutMapping("/students/{studentId}")
    public Result<StudentDetailVO> updateStudent(@PathVariable Long studentId,
                                                 @RequestBody @Valid StudentUpdateDTO dto) {
        return Result.success(settingsStudentService.updateStudent(studentId, dto));
    }

    /** 删除学员（软删除） */
    @DeleteMapping("/students/{studentId}")
    public Result<StudentDeleteVO> deleteStudent(@PathVariable Long studentId) {
        return Result.success(settingsStudentService.deleteStudent(studentId));
    }

    /** 查询学员课程进度图 */
    @GetMapping("/students/{studentId}/course-progress")
    public Result<CourseProgressVO> getCourseProgress(@PathVariable Long studentId) {
        return Result.success(settingsStudentService.getCourseProgress(studentId));
    }

    /** 推满学员某门课程进度 */
    @PatchMapping("/students/{studentId}/courses/{courseId}/progress/complete")
    public Result<ProgressCompleteVO> completeProgress(@PathVariable Long studentId,
                                                       @PathVariable Long courseId) {
        return Result.success(settingsStudentService.completeProgress(studentId, courseId));
    }

    /** 清零学员某门课程进度 */
    @PatchMapping("/students/{studentId}/courses/{courseId}/progress/reset")
    public Result<ProgressResetVO> resetProgress(@PathVariable Long studentId,
                                                 @PathVariable Long courseId) {
        return Result.success(settingsStudentService.resetProgress(studentId, courseId));
    }

    /** 查询科室下拉选项 */
    @GetMapping("/departments/options")
    public Result<List<DepartmentOptionVO>> getDepartmentOptions() {
        return Result.success(settingsStudentService.getDepartmentOptions());
    }
}
