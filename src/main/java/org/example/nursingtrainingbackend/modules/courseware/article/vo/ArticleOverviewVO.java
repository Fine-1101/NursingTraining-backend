package org.example.nursingtrainingbackend.modules.courseware.article.vo;

import lombok.Data;

/**
 * 文章概览统计VO
 */
@Data
public class ArticleOverviewVO {
    
    /**
     * 未删除文章总数
     */
    private Long totalArticles;
    
    /**
     * 已发布文章数
     */
    private Long publishedArticles;
    
    /**
     * 草稿文章数
     */
    private Long draftArticles;
    
    /**
     * 本月文章浏览总次数
     */
    private Long monthlyViews;
    
    /**
     * 环比数据，无历史快照时为null
     */
    private MonthOverMonth monthOverMonth;
    
    /**
     * 环比数据内部类
     */
    @Data
    public static class MonthOverMonth {
        /**
         * 文章总数环比百分比
         */
        private Double totalArticlesRate;
        
        /**
         * 已发布文章环比百分比
         */
        private Double publishedArticlesRate;
        
        /**
         * 草稿文章环比百分比
         */
        private Double draftArticlesRate;
        
        /**
         * 本月浏览量环比百分比
         */
        private Double monthlyViewsRate;
    }
}
