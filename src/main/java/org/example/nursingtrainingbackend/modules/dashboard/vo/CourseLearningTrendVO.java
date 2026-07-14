package org.example.nursingtrainingbackend.modules.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单门课程学习趋势响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseLearningTrendVO {

    private Long courseId;
    private String courseTitle;
    private String range;
    private String granularity;
    private List<Point> points;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        private String label;
        private String date;
        private Integer learnerCount;
        private BigDecimal completionRate;
    }
}
