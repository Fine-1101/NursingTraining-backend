package org.example.nursingtrainingbackend.modules.courseware.article.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleBatchRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleStatusUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleUploadRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.vo.*;
import org.example.nursingtrainingbackend.security.AuthenticatedUser;

/**
 * 文章管理服务接口
 */
public interface ArticleService {
    
    /**
     * 分页查询文章列表
     */
    PageResult<ArticleListItemVO> listArticles(String keyword, String status, 
                                                String uploadedFrom, String uploadedTo,
                                                String sortOrder, Integer page, Integer size);
    
    /**
     * 查询文章概览统计
     */
    ArticleOverviewVO getArticleOverview();
    
    /**
     * 查询文章详情
     */
    ArticleDetailVO getArticleDetail(Long id);
    
    /**
     * 预览文章
     */
    ArticlePreviewVO previewArticle(Long id);
    
    /**
     * 上传文章
     */
    ArticleUploadResponseVO uploadArticle(ArticleUploadRequest request, AuthenticatedUser user);
    
    /**
     * 编辑文章
     */
    ArticleUpdateResponseVO updateArticle(Long id, ArticleUpdateRequest request);
    
    /**
     * 修改文章状态
     */
    ArticleStatusUpdateResponseVO updateArticleStatus(Long id, ArticleStatusUpdateRequest request);
    
    /**
     * 删除单篇文章
     */
    void deleteArticle(Long id);
    
    /**
     * 批量发布文章
     */
    ArticleBatchPublishResponseVO batchPublishArticles(ArticleBatchRequest request);
    
    /**
     * 批量删除文章
     */
    ArticleBatchDeleteResponseVO batchDeleteArticles(ArticleBatchRequest request);
}
