package org.example.nursingtrainingbackend.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.nursingtrainingbackend.modules.system.vo.CourseProgressItemVO;
import org.example.nursingtrainingbackend.modules.system.vo.DepartmentDistributionItemVO;
import org.example.nursingtrainingbackend.modules.system.vo.DepartmentOptionVO;
import org.example.nursingtrainingbackend.modules.system.vo.StudentListItemVO;
import org.example.nursingtrainingbackend.modules.user.entity.User;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface SettingsStudentMapper extends BaseMapper<User> {

    /** 分页查询学员列表 */
    IPage<StudentListItemVO> selectStudentPage(Page<StudentListItemVO> page,
                                               @Param("keyword") String keyword,
                                               @Param("departmentId") Long departmentId);

    /** 根据科室ID查科室名称 */
    String selectDepartmentNameById(@Param("deptId") Long deptId);

    /** 根据科室ID查科室状态 */
    Integer selectDepartmentStatusById(@Param("deptId") Long deptId);

    /** 查询学员按科室分布 */
    List<DepartmentDistributionItemVO> selectDepartmentDistribution(@Param("activeOnly") boolean activeOnly);

    /** 查询学员可学习课程的进度 */
    List<CourseProgressItemVO> selectStudentCourseProgress(@Param("userId") Long userId,
                                                           @Param("deptId") Long deptId);

    /** 查询科室下拉选项 */
    List<DepartmentOptionVO> selectDepartmentOptions();

    /** 查询已发布且属于该学员科室的课程 */
    Integer selectPublishedCourseInDept(@Param("courseId") Long courseId,
                                        @Param("deptId") Long deptId);

    /** 查询单个学员的平均学习进度（从 user_course_progress 直接计算） */
    BigDecimal selectAvgProgressByUserId(@Param("userId") Long userId);

    /** 批量查询多个学员的平均学习进度 */
    List<Map<String, Object>> selectAvgProgressBatch(@Param("userIds") Collection<Long> userIds);
}
