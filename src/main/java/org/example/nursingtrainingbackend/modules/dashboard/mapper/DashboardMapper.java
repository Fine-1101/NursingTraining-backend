package org.example.nursingtrainingbackend.modules.dashboard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.nursingtrainingbackend.modules.dashboard.dto.CourseTrendRow;
import org.example.nursingtrainingbackend.modules.dashboard.dto.DepartmentRankRow;
import org.example.nursingtrainingbackend.modules.dashboard.dto.StatusCountRow;
import org.example.nursingtrainingbackend.modules.dashboard.dto.TrendRow;

import java.util.List;

@Mapper
public interface DashboardMapper {

    /** 统计未删除课程总数 */
    int countCourses();

    /** 统计学员总数（role_type=1, 未删除） */
    int countLearners();

    /** 按状态统计学员学习进度分布 */
    List<StatusCountRow> countProgressByStatus();

    /** 按月份统计学习趋势 */
    List<TrendRow> selectMonthlyLearningTrend(@Param("startDate") String startDate,
                                               @Param("endDate") String endDate);

    /** 按周统计学习趋势 */
    List<TrendRow> selectWeeklyLearningTrend(@Param("startDate") String startDate,
                                              @Param("endDate") String endDate);

    /** 查询某门课程按月的学习人数与完成率趋势 */
    List<CourseTrendRow> selectCourseMonthlyTrend(@Param("courseId") Long courseId);

    /** 查询某门课程在指定日期范围内的按月学习人数与完成率趋势 */
    List<CourseTrendRow> selectCourseTrendByRange(@Param("courseId") Long courseId,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate,
                                                   @Param("granularity") String granularity);

    /** 科室完成率排行 */
    List<DepartmentRankRow> selectDepartmentRanking(@Param("limit") int limit);

    /** 查询某日期的课程快照总数 */
    Integer selectLastMonthEndCourseSnapshot(@Param("statDate") String statDate);

    /** 查询某日期之前注册的学员数 */
    Integer selectLearnerCountBefore(@Param("beforeDate") String beforeDate);

    /** 按周统计指定月份内的学习趋势（下钻第二层） */
    List<TrendRow> selectWeeklyTrendInMonth(@Param("year") int year,
                                            @Param("month") int month);

    /** 按天统计指定周内的学习趋势（下钻第三层） */
    List<TrendRow> selectDailyTrendInWeek(@Param("startDate") String startDate,
                                          @Param("endDate") String endDate);
}
