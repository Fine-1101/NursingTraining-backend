package org.example.nursingtrainingbackend.modules.system.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.system.dto.StudentQueryDTO;
import org.example.nursingtrainingbackend.modules.system.dto.StudentUpdateDTO;
import org.example.nursingtrainingbackend.modules.system.vo.*;

import java.util.List;

public interface SettingsStudentService {

    /** 查询当前管理员信息 */
    CurrentUserVO getCurrentUser();

    /** 分页查询学员列表 */
    PageResult<StudentListItemVO> queryStudents(StudentQueryDTO query);

    /** 查询学员详情 */
    StudentDetailVO getStudentDetail(Long studentId);

    /** 编辑学员信息 */
    StudentDetailVO updateStudent(Long studentId, StudentUpdateDTO dto);

    /** 删除学员（软删除） */
    StudentDeleteVO deleteStudent(Long studentId);

    /** 查询学员按科室分布 */
    DepartmentDistributionVO getDepartmentDistribution(boolean activeOnly);

    /** 查询学员课程进度图 */
    CourseProgressVO getCourseProgress(Long studentId);

    /** 推满学员某门课程进度 */
    ProgressCompleteVO completeProgress(Long studentId, Long courseId);

    /** 清零学员某门课程进度 */
    ProgressResetVO resetProgress(Long studentId, Long courseId);

    /** 查询科室下拉选项 */
    List<DepartmentOptionVO> getDepartmentOptions();
}
