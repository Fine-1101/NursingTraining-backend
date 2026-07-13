package org.example.nursingtrainingbackend.modules.dashboard.service;

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
    void evictDashboardCache();
}
