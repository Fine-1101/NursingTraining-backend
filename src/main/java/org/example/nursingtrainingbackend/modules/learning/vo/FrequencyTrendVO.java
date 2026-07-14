package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FrequencyTrendVO {

    private String unit;

    private BigDecimal averageCount;

    private List<TrendPoint> points;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String label;
        private String date;
        private Integer hour;
        private Integer count;
    }
}
