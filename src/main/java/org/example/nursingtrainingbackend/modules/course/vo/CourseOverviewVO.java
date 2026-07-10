package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

@Data
public class CourseOverviewVO {
    private StatItem total;
    private StatItem draft;
    private StatItem published;
    private StatItem offline;

    @Data
    public static class StatItem {
        private Integer value;
        private Double changeRate;
        private String changeDirection;

        public StatItem() {}
        public StatItem(Integer value) {
            this.value = value;
        }
    }
}
