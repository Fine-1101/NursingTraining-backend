package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.util.List;

/**
 * 学员首页聚合响应VO
 */
@Data
public class HomePageVO {

    /** 顶部课程状态统计 */
    private CourseStatsVO courseStats;

    /** 首页推荐课程列表，最多4条 */
    private List<RecommendedCourseVO> recommendedCourses;

    /** 首页继续学习列表，最多4条 */
    private List<ContinueCourseVO> continueCourses;

    /** 学习进度概览 */
    private ProgressOverviewVO progressOverview;

    /** 最近学习记录，最多5条 */
    private List<LearningRecordVO> recentRecords;

    /** 当前月份学习日历 */
    private CalendarVO calendar;
}
