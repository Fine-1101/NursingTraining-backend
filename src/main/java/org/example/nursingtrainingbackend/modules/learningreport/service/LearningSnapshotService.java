package org.example.nursingtrainingbackend.modules.learningreport.service;

import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;

import java.time.LocalDateTime;

public interface LearningSnapshotService {

    /**
     * 根据用户和统计周期生成周学习报告快照。
     *
     * @param userId         用户ID，由上层从JWT获取
     * @param requestedStart 用户请求的开始时间，可以为null
     * @param requestedEnd   用户请求的结束时间，可以为null
     * @return 学习报告快照
     */
    LearningReportSnapshot buildWeeklySnapshot(
            Long userId,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd
    );
}
