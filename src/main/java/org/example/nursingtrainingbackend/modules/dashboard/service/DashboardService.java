package org.example.nursingtrainingbackend.modules.dashboard.service;

import org.example.nursingtrainingbackend.modules.dashboard.vo.CourseLearningTrendVO;
import org.example.nursingtrainingbackend.modules.dashboard.vo.DashboardVO;

public interface DashboardService {
    /**
     * 获取首页面板聚合数据
     *
     * @param range           图表统计范围：LAST_6_WEEKS / LAST_6_MONTHS / LAST_12_MONTHS
     * @param departmentLimit 科室排行展示数量
     * @return 面板数据
     */
    DashboardVO getDashboard(String range, int departmentLimit);

    /**
     * 获取单门课程学习趋势
     *
     * @param courseId 课程 ID
     * @param range    时间范围：LAST_1_WEEKS / LAST_1_MONTHS / LAST_6_MONTHS
     * @return 课程学习趋势数据
     */
    CourseLearningTrendVO getCourseLearningTrend(Long courseId, String range, String granularity);

    void evictDashboardCache();
}
