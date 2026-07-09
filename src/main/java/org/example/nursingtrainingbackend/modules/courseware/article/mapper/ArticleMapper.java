package org.example.nursingtrainingbackend.modules.courseware.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.Article;

/**
 * 文章数据访问层
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    
    /**
     * 统计未删除文章总数
     */
    @Select("SELECT COUNT(*) FROM article WHERE deleted_at IS NULL")
    long countTotalArticles();
    
    /**
     * 统计已发布文章数
     */
    @Select("SELECT COUNT(*) FROM article WHERE status = 1 AND deleted_at IS NULL")
    long countPublishedArticles();
    
    /**
     * 统计草稿文章数
     */
    @Select("SELECT COUNT(*) FROM article WHERE status = 0 AND deleted_at IS NULL")
    long countDraftArticles();
}
