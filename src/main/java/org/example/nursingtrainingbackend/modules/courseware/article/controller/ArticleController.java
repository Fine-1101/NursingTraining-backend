package org.example.nursingtrainingbackend.modules.courseware.article.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleBatchRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleStatusUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleUploadRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.service.ArticleService;
import org.example.nursingtrainingbackend.modules.courseware.article.vo.*;
import org.example.nursingtrainingbackend.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 文章管理控制器
 */
@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class ArticleController {
    
    private final ArticleService articleService;
    
    /**
     * 分页查询文章列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<ArticleListItemVO>> listArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String uploadedFrom,
            @RequestParam(required = false) String uploadedTo,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        PageResult<ArticleListItemVO> result = articleService.listArticles(
                keyword, status, uploadedFrom, uploadedTo, sortOrder, page, size);
        return Result.success(result);
    }
    
    /**
     * 查询文章概览统计
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ArticleOverviewVO> getArticleOverview() {
        ArticleOverviewVO overview = articleService.getArticleOverview();
        return Result.success(overview);
    }
    
    /**
     * 预览文章
     */
    @GetMapping("/{id}/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ArticlePreviewVO> previewArticle(@PathVariable Long id) {
        ArticlePreviewVO preview = articleService.previewArticle(id);
        return Result.success(preview);
    }
    
    /**
     * 查询文章详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ArticleDetailVO> getArticleDetail(@PathVariable Long id) {
        ArticleDetailVO detail = articleService.getArticleDetail(id);
        return Result.success(detail);
    }
    
    /**
     * 上传文章
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ArticleUploadResponseVO> uploadArticle(
            @Valid @RequestBody ArticleUploadRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        
        ArticleUploadResponseVO response = articleService.uploadArticle(request, user);
        return Result.success(response);
    }
    
    /**
     * 批量发布文章
     */
    @PostMapping("/batch-publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ArticleBatchPublishResponseVO> batchPublishArticles(
            @Valid @RequestBody ArticleBatchRequest request) {
        
        ArticleBatchPublishResponseVO response = articleService.batchPublishArticles(request);
        return Result.success(response);
    }
    
    /**
     * 编辑文章
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ArticleUpdateResponseVO> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleUpdateRequest request) {
        
        ArticleUpdateResponseVO response = articleService.updateArticle(id, request);
        return Result.success(response);
    }
    
    /**
     * 修改文章状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ArticleStatusUpdateResponseVO> updateArticleStatus(
            @PathVariable Long id,
            @Valid @RequestBody ArticleStatusUpdateRequest request) {
        
        ArticleStatusUpdateResponseVO response = articleService.updateArticleStatus(id, request);
        return Result.success(response);
    }
    
    /**
     * 删除单篇文章
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.success();
    }
    
    /**
     * 批量删除文章
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ArticleBatchDeleteResponseVO> batchDeleteArticles(
            @Valid @RequestBody ArticleBatchRequest request) {
        
        ArticleBatchDeleteResponseVO response = articleService.batchDeleteArticles(request);
        return Result.success(response);
    }
}
