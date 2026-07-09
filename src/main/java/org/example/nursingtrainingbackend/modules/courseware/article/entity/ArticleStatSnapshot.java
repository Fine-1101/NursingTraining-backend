package org.example.nursingtrainingbackend.modules.courseware.article.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 文章统计快照实体类
 */
@Data
@TableName("article_stat_snapshot")
public class ArticleStatSnapshot {

    /**
     * 统计日期（主键）
     */
    @TableId
    private LocalDate statDate;

    /**
     * 文章总数
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
     * 当月累计浏览量
     */
    private Long monthlyViews;
}
