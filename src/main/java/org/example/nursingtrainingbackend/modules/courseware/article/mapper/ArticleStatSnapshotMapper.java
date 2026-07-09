package org.example.nursingtrainingbackend.modules.courseware.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.ArticleStatSnapshot;

import java.time.LocalDate;

/**
 * 文章统计快照数据访问层
 */
@Mapper
public interface ArticleStatSnapshotMapper extends BaseMapper<ArticleStatSnapshot> {

    /**
     * 插入或更新快照（使用 ON DUPLICATE KEY UPDATE）
     */
    @Update("INSERT INTO article_stat_snapshot (stat_date, total_articles, published_articles, draft_articles, monthly_views) " +
            "VALUES (#{statDate}, #{totalArticles}, #{publishedArticles}, #{draftArticles}, #{monthlyViews}) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_articles = VALUES(total_articles), " +
            "published_articles = VALUES(published_articles), " +
            "draft_articles = VALUES(draft_articles), " +
            "monthly_views = VALUES(monthly_views)")
    void upsertSnapshot(@Param("statDate") LocalDate statDate,
                        @Param("totalArticles") long totalArticles,
                        @Param("publishedArticles") long publishedArticles,
                        @Param("draftArticles") long draftArticles,
                        @Param("monthlyViews") long monthlyViews);

    /**
     * 查询指定日期的快照
     */
    @Select("SELECT * FROM article_stat_snapshot WHERE stat_date = #{statDate}")
    ArticleStatSnapshot selectByDate(@Param("statDate") LocalDate statDate);
}
