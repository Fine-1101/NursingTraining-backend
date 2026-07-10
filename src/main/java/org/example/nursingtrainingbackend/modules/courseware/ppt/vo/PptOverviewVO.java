package org.example.nursingtrainingbackend.modules.courseware.ppt.vo;

import lombok.Builder;
import lombok.Data;

/**
 * PPT概览统计VO
 * 对应前端 PptManagement.vue 中 metrics 计算属性所需的字段
 */
@Data
@Builder
public class PptOverviewVO {

    /**
     * PPT 总数（未删除）
     */
    private Long totalPpts;

    /**
     * 已发布 PPT 数
     */
    private Long publishedPpts;

    /**
     * 草稿 PPT 数
     */
    private Long draftPpts;

    /**
     * 本月新增 PPT 数
     */
    private Long monthlyAdded;

    /**
     * 环比数据，无历史数据时为 null
     */
    private MonthOverMonth monthOverMonth;

    @Data
    @Builder
    public static class MonthOverMonth {
        /**
         * PPT 总数环比百分比
         */
        private Double totalPptsRate;

        /**
         * 已发布 PPT 环比百分比
         */
        private Double publishedPptsRate;

        /**
         * 草稿 PPT 环比百分比
         */
        private Double draftPptsRate;

        /**
         * 本月新增环比百分比
         */
        private Double monthlyAddedRate;
    }
}
