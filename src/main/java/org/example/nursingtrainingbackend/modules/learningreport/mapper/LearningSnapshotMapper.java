package org.example.nursingtrainingbackend.modules.learningreport.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningOverviewRow;

import java.time.LocalDateTime;

/**
 * AI学习报告数据统计 Mapper。
 */
@Mapper
public interface LearningSnapshotMapper {

    /**
     * 查询用户在指定时间范围内的学习概览。
     */
    LearningOverviewRow selectLearningOverview(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}