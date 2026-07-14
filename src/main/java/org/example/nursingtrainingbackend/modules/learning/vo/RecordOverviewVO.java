package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.util.List;

@Data
public class RecordOverviewVO {

    private SummaryCards summaryCards;

    private List<ResourceDistributionVO> resourceDistribution;

    private FrequencyTrendVO frequencyTrend;

    private List<TopCourseVO> topCourses;

    @Data
    public static class SummaryCards {
        private Integer recordCount;
        private Integer completedResourceCount;
        private Integer completedPointCount;
        private Integer learningDays;
    }
}
