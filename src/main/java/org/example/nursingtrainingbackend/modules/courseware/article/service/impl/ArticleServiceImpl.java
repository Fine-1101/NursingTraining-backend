package org.example.nursingtrainingbackend.modules.courseware.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.config.properties.ArticleAttachmentProperties;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleBatchRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleStatusUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleUploadRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.dto.ArticleUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.Article;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.ArticleStatSnapshot;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleMapper;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleStatSnapshotMapper;
import org.example.nursingtrainingbackend.modules.courseware.article.service.ArticleService;
import org.example.nursingtrainingbackend.modules.courseware.article.vo.*;

import org.example.nursingtrainingbackend.modules.file.service.FileService;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * 文章管理服务实现
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {
    
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final ArticleAttachmentProperties attachmentProperties;
    private final ArticleStatSnapshotMapper snapshotMapper;

    private final FileService fileService;
    /** 分页或按条件查询文章。 */
    
    @Override
    public PageResult<ArticleListItemVO> listArticles(String keyword, String status, 
                                                       String uploadedFrom, String uploadedTo,
                                                       String sortOrder, Integer page, Integer size) {
        // 参数校验和默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }
        if (size > 100) {
            size = 100;
        }
        
        // 构建查询条件
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询未删除的文章
        wrapper.isNull(Article::getDeletedAt);
        
        // 关键词搜索（标题、摘要）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Article::getTitle, keyword)
                    .or().like(Article::getSummary, keyword));
        }
        
        // 状态筛选
        if (StringUtils.hasText(status)) {
            Integer statusCode = convertStatusToCode(status);
            if (statusCode != null) {
                wrapper.eq(Article::getStatus, statusCode);
            }
        }
        
        // 上传日期范围
        if (StringUtils.hasText(uploadedFrom)) {
            LocalDateTime fromDateTime = LocalDate.parse(uploadedFrom).atStartOfDay();
            wrapper.ge(Article::getCreatedAt, fromDateTime);
        }
        if (StringUtils.hasText(uploadedTo)) {
            LocalDateTime toDateTime = LocalDate.parse(uploadedTo).atTime(LocalTime.MAX);
            wrapper.le(Article::getCreatedAt, toDateTime);
        }
        
        // 排序：默认按created_at DESC, id DESC
        if ("asc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(Article::getCreatedAt).orderByAsc(Article::getId);
        } else {
            wrapper.orderByDesc(Article::getCreatedAt).orderByDesc(Article::getId);
        }
        
        // 执行分页查询
        Page<Article> pageParam = new Page<>(page, size);
        Page<Article> resultPage = articleMapper.selectPage(pageParam, wrapper);
        
        // 转换为VO
        List<ArticleListItemVO> voList = resultPage.getRecords().stream()
                .map(this::convertToListItemVO)
                .toList();
        
        return new PageResult<>(voList, resultPage.getTotal(), 
                resultPage.getCurrent(), resultPage.getSize(), resultPage.getPages());
    }
    /** 获取文章概览。 */
    
    @Override
    public ArticleOverviewVO getArticleOverview() {
        ArticleOverviewVO overview = new ArticleOverviewVO();
        
        // 查询实时统计数据
        long totalArticles = articleMapper.countTotalArticles();
        long publishedArticles = articleMapper.countPublishedArticles();
        long draftArticles = articleMapper.countDraftArticles();
        
        overview.setTotalArticles(totalArticles);
        overview.setPublishedArticles(publishedArticles);
        overview.setDraftArticles(draftArticles);
        
        // 当前阶段不提供浏览量采集接口，monthlyViews 暂时设为 0
        overview.setMonthlyViews(0L);

        // 环比计算：与上月同日快照比较
        LocalDate lastMonthSameDay = LocalDate.now().minusMonths(1);
        
        // 如果上月同日不存在（如3月31日 -> 2月没有31日），取上月最后一天
        if (lastMonthSameDay.getMonthValue() != LocalDate.now().getMonthValue() - 1 &&
            lastMonthSameDay.getMonthValue() != 12) {
            lastMonthSameDay = lastMonthSameDay.withDayOfMonth(lastMonthSameDay.lengthOfMonth());
        }
        
        ArticleStatSnapshot lastMonthSnapshot = snapshotMapper.selectByDate(lastMonthSameDay);
        
        if (lastMonthSnapshot != null) {
            ArticleOverviewVO.MonthOverMonth monthOverMonth = new ArticleOverviewVO.MonthOverMonth();
            
            monthOverMonth.setTotalArticlesRate(calculateRate(totalArticles, lastMonthSnapshot.getTotalArticles()));
            monthOverMonth.setPublishedArticlesRate(calculateRate(publishedArticles, lastMonthSnapshot.getPublishedArticles()));
            monthOverMonth.setDraftArticlesRate(calculateRate(draftArticles, lastMonthSnapshot.getDraftArticles()));
            monthOverMonth.setMonthlyViewsRate(calculateRate(overview.getMonthlyViews(), lastMonthSnapshot.getMonthlyViews()));
            
            overview.setMonthOverMonth(monthOverMonth);
        } else {
            // 无历史快照时环比返回null
            overview.setMonthOverMonth(null);
        }
        
        return overview;
    }
    
    /**
     * 计算环比百分比
     * 公式：(当前值 - 上月同期值) / 上月同期值 × 100%
     * 上月同期值为0或没有快照时返回null
     */
    private Double calculateRate(long currentValue, long previousValue) {
        if (previousValue == 0) {
            return null;
        }
        return ((double)(currentValue - previousValue) / previousValue) * 100.0;
    }
    /** 获取文章详情。 */
    
    @Override
    public ArticleDetailVO getArticleDetail(Long id) {
        // 手动查询未删除的文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getId, id).isNull(Article::getDeletedAt);
        Article article = articleMapper.selectOne(wrapper);
        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        
        return convertToDetailVO(article);
    }
    /** 获取文章预览数据。 */
    
    @Override
    public ArticlePreviewVO previewArticle(Long id) {
        // 手动查询未删除的文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getId, id).isNull(Article::getDeletedAt);
        Article article = articleMapper.selectOne(wrapper);
        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        
        return convertToPreviewVO(article);
    }
    /** 上传并登记文章。 */
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleUploadResponseVO uploadArticle(ArticleUploadRequest request, AuthenticatedUser user) {
        // 校验标题
        String title = request.getTitle().trim();
        if (title.isEmpty()) {
            throw new BusinessException(ErrorCode.ARTICLE_TITLE_INVALID);
        }
        
        // 校验正文
        String content = cleanHtmlContent(request.getContent());
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.ARTICLE_CONTENT_EMPTY);
        }
        
        // 校验附件信息完整性
        validateAttachmentInfo(request.getAttachmentUrl(), request.getAttachmentName(), 
                request.getAttachmentSize(), request.getAllowDownload());
        
        // 创建文章实体
        Article article = new Article();
        article.setTitle(title);
        article.setSummary(request.getSummary());
        article.setContent(content);
        article.setCoverUrl(request.getCoverUrl());
        article.setAttachmentUrl(request.getAttachmentUrl());
        article.setAttachmentName(request.getAttachmentName());
        article.setAttachmentSize(request.getAttachmentSize());
        article.setAllowDownload(request.getAllowDownload() ? 1 : 0);
        article.setViewCount(0);
        article.setReadCount(0);
        article.setCreatedBy(user.id());
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        
        // 处理状态
        String statusStr = request.getStatus();
        if ("PUBLISHED".equalsIgnoreCase(statusStr)) {
            article.setStatus(1);
            article.setPublishedAt(LocalDateTime.now());
        } else {
            article.setStatus(0);
        }
        
        // 保存文章
        articleMapper.insert(article);
        
        // 标记封面和附件文件已使用
        if (request.getCoverUrl() != null && !request.getCoverUrl().isBlank()) {
            String coverKey = extractObjectKey(request.getCoverUrl());
            fileService.markFileUsed(coverKey, "ARTICLE_COVER", article.getId());
        }
        if (request.getAttachmentUrl() != null && !request.getAttachmentUrl().isBlank()) {
            String attachmentKey = extractObjectKey(request.getAttachmentUrl());
            fileService.markFileUsed(attachmentKey, "ARTICLE_ATTACHMENT", article.getId());
        }
        
        // 构建响应
        ArticleUploadResponseVO response = new ArticleUploadResponseVO();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setUploaderId(user.id());
        response.setUploaderName(user.nickname());
        response.setStatus(convertCodeToStatus(article.getStatus()));
        response.setUploadedAt(article.getCreatedAt());
        response.setPublishedAt(article.getPublishedAt());
        
        return response;
    }
    /** 更新文章。 */
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleUpdateResponseVO updateArticle(Long id, ArticleUpdateRequest request) {
        // 查找未删除的文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getId, id).isNull(Article::getDeletedAt);
        Article article = articleMapper.selectOne(wrapper);
        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        
        // 校验标题
        String title = request.getTitle().trim();
        if (title.isEmpty()) {
            throw new BusinessException(ErrorCode.ARTICLE_TITLE_INVALID);
        }
        
        // 校验正文
        String content = cleanHtmlContent(request.getContent());
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.ARTICLE_CONTENT_EMPTY);
        }
        
        // 校验附件信息完整性
        validateAttachmentInfo(request.getAttachmentUrl(), request.getAttachmentName(), 
                request.getAttachmentSize(), request.getAllowDownload());
        
        // 使用 LambdaUpdateWrapper 显式更新所有字段（包括可能为 null 的字段）
        LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Article::getId, id)
                .set(Article::getTitle, title)
                .set(Article::getSummary, request.getSummary())
                .set(Article::getContent, content)
                .set(Article::getCoverUrl, request.getCoverUrl())
                .set(Article::getAttachmentUrl, request.getAttachmentUrl())
                .set(Article::getAttachmentName, request.getAttachmentName())
                .set(Article::getAttachmentSize, request.getAttachmentSize())
                .set(Article::getUpdatedAt, LocalDateTime.now());
        
        // 处理 allowDownload
        if (request.getAllowDownload() != null) {
            updateWrapper.set(Article::getAllowDownload, request.getAllowDownload() ? 1 : 0);
        }
        
        // 更新数据库
        articleMapper.update(null, updateWrapper);
        
        // 构建响应
        ArticleUpdateResponseVO response = new ArticleUpdateResponseVO();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setStatus(convertCodeToStatus(article.getStatus()));
        response.setUpdatedAt(article.getUpdatedAt());
        
        return response;
    }
    /** 更新文章状态。 */
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleStatusUpdateResponseVO updateArticleStatus(Long id, ArticleStatusUpdateRequest request) {
        // 查找未删除的文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getId, id).isNull(Article::getDeletedAt);
        Article article = articleMapper.selectOne(wrapper);
        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        
        // 解析目标状态
        String targetStatusStr = request.getStatus().toUpperCase();
        Integer targetStatusCode = convertStatusToCode(targetStatusStr);
        if (targetStatusCode == null) {
            throw new BusinessException(ErrorCode.ARTICLE_STATUS_INVALID);
        }
        
        // 检查状态是否相同
        if (article.getStatus().equals(targetStatusCode)) {
            throw new BusinessException(ErrorCode.ARTICLE_STATUS_INVALID);
        }
        
        // 验证状态转换合法性
        validateStatusTransition(article.getStatus(), targetStatusCode);
        
        // 更新状态
        article.setStatus(targetStatusCode);
        article.setUpdatedAt(LocalDateTime.now());
        
        // 根据状态更新published_at
        if (targetStatusCode == 1) { // PUBLISHED
            article.setPublishedAt(LocalDateTime.now());
        } else if (targetStatusCode == 0) { // DRAFT
            article.setPublishedAt(null);
        }
        // OFFLINE时保留publishedAt
        
        articleMapper.updateById(article);
        
        // 构建响应
        ArticleStatusUpdateResponseVO response = new ArticleStatusUpdateResponseVO();
        response.setId(article.getId());
        response.setStatus(convertCodeToStatus(article.getStatus()));
        response.setPublishedAt(article.getPublishedAt());
        response.setUpdatedAt(article.getUpdatedAt());
        
        return response;
    }
    /** 删除文章。 */
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id) {
        // 查找未删除的文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getId, id).isNull(Article::getDeletedAt);
        Article article = articleMapper.selectOne(wrapper);
        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        
        // 软删除：使用 LambdaUpdateWrapper 显式设置 deleted_at
        LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Article::getId, id)
                .set(Article::getDeletedAt, LocalDateTime.now());
        articleMapper.update(null, updateWrapper);
    }
    /** 批量发布文章。 */
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleBatchPublishResponseVO batchPublishArticles(ArticleBatchRequest request) {
        List<Long> ids = request.getIds();
        LocalDateTime publishTime = LocalDateTime.now();
        
        // 查询所有未删除的文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Article::getId, ids).isNull(Article::getDeletedAt);
        List<Article> articles = articleMapper.selectList(wrapper);
        
        // 检查是否所有ID都存在
        if (articles.size() != ids.size()) {
            throw new BusinessException(ErrorCode.ARTICLE_BATCH_PUBLISH_FAILED);
        }
        
        // 检查每篇文章的状态是否允许发布（只允许草稿或已下架）
        for (Article article : articles) {
            if (article.getStatus() != 0 && article.getStatus() != 2) { // 不是草稿且不是已下架
                throw new BusinessException(ErrorCode.ARTICLE_BATCH_PUBLISH_FAILED);
            }
        }
        
        // 批量更新状态为已发布
        for (Article article : articles) {
            article.setStatus(1); // PUBLISHED
            article.setPublishedAt(publishTime);
            article.setUpdatedAt(publishTime);
            articleMapper.updateById(article);
        }
        
        // 构建响应
        ArticleBatchPublishResponseVO response = new ArticleBatchPublishResponseVO();
        response.setRequestedCount(ids.size());
        response.setPublishedCount(articles.size());
        response.setPublishedAt(publishTime);
        
        return response;
    }
    /** 批量删除文章。 */
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleBatchDeleteResponseVO batchDeleteArticles(ArticleBatchRequest request) {
        List<Long> ids = request.getIds();
        LocalDateTime deleteTime = LocalDateTime.now();
        
        // 查询所有未删除的文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Article::getId, ids).isNull(Article::getDeletedAt);
        List<Article> articles = articleMapper.selectList(wrapper);
        
        // 检查是否所有ID都存在
        if (articles.size() != ids.size()) {
            throw new BusinessException(ErrorCode.ARTICLE_BATCH_DELETE_FAILED);
        }
        
        // 批量软删除：使用 LambdaUpdateWrapper 显式更新 deletedAt 字段
        for (Article article : articles) {
            LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Article::getId, article.getId())
                    .set(Article::getDeletedAt, deleteTime);
            articleMapper.update(null, updateWrapper);
        }
        
        // 构建响应
        ArticleBatchDeleteResponseVO response = new ArticleBatchDeleteResponseVO();
        response.setRequestedCount(ids.size());
        response.setDeletedCount(articles.size());
        
        return response;
    }
    
    /**
     * 转换为列表项VO
     */
    private ArticleListItemVO convertToListItemVO(Article article) {
        ArticleListItemVO vo = new ArticleListItemVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setUploaderId(article.getCreatedBy());
        vo.setUploaderName(getUploaderName(article.getCreatedBy()));
        vo.setUploadedAt(article.getCreatedAt());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setViewCount(article.getViewCount());
        vo.setStatus(convertCodeToStatus(article.getStatus()));
        vo.setHasAttachment(StringUtils.hasText(article.getAttachmentUrl()));
        return vo;
    }
    
    /**
     * 转换为详情VO
     */
    private ArticleDetailVO convertToDetailVO(Article article) {
        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setCoverUrl(article.getCoverUrl());
        vo.setAttachmentUrl(article.getAttachmentUrl());
        vo.setAttachmentName(article.getAttachmentName());
        vo.setAttachmentSize(article.getAttachmentSize());
        vo.setAllowDownload(article.getAllowDownload() == 1);
        vo.setUploaderId(article.getCreatedBy());
        vo.setUploaderName(getUploaderName(article.getCreatedBy()));
        vo.setViewCount(article.getViewCount());
        vo.setStatus(convertCodeToStatus(article.getStatus()));
        vo.setUploadedAt(article.getCreatedAt());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        return vo;
    }
    
    /**
     * 转换为预览VO
     */
    private ArticlePreviewVO convertToPreviewVO(Article article) {
        ArticlePreviewVO vo = new ArticlePreviewVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(cleanHtmlContent(article.getContent()));
        vo.setCoverUrl(article.getCoverUrl());
        vo.setUploaderName(getUploaderName(article.getCreatedBy()));
        vo.setStatus(convertCodeToStatus(article.getStatus()));
        vo.setAllowDownload(article.getAllowDownload() == 1);
        vo.setAttachmentName(article.getAttachmentName());
        return vo;
    }
    
    /**
     * 获取上传者姓名
     */
    private String getUploaderName(Long userId) {
        if (userId == null) {
            return "未知用户";
        }
        User user = userMapper.selectById(userId);
        return user != null ? user.getRealName() : "未知用户";
    }
    
    /**
     * 状态字符串转数字
     */
    private Integer convertStatusToCode(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "DRAFT" -> 0;
            case "PUBLISHED" -> 1;
            case "OFFLINE" -> 2;
            default -> null;
        };
    }
    
    /**
     * 状态数字转字符串
     */
    private String convertCodeToStatus(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> "DRAFT";
            case 1 -> "PUBLISHED";
            case 2 -> "OFFLINE";
            default -> null;
        };
    }
    
    /**
     * 清洗HTML内容（简单实现，移除script标签和事件属性）
     */
    private String cleanHtmlContent(String html) {
        if (!StringUtils.hasText(html)) {
            return html;
        }
        // 移除script标签及其内容
        String cleaned = html.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        // 移除on*事件属性
        cleaned = cleaned.replaceAll("\\s+on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "");
        cleaned = cleaned.replaceAll("\\s+on\\w+\\s*=\\s*[^\\s>]+", "");
        return cleaned.trim();
    }
    
    /**
     * 校验附件信息完整性和合法性
     */
    private void validateAttachmentInfo(String attachmentUrl, String attachmentName, 
                                        Long attachmentSize, Boolean allowDownload) {
        boolean hasAttachment = StringUtils.hasText(attachmentUrl);
        boolean hasName = StringUtils.hasText(attachmentName);
        boolean hasSize = attachmentSize != null && attachmentSize > 0;
        
        // 如果有附件URL，必须有名称和大小
        if (hasAttachment && (!hasName || !hasSize)) {
            throw new BusinessException(ErrorCode.ARTICLE_ATTACHMENT_INVALID);
        }
        
        // 如果允许下载，必须有完整附件信息
        if (Boolean.TRUE.equals(allowDownload) && !hasAttachment) {
            throw new BusinessException(ErrorCode.ARTICLE_NO_ATTACHMENT);
        }
        
        // 如果有附件，进行深度验证
        if (hasAttachment) {
            // 1. OSS域名校验
            validateOssDomain(attachmentUrl);
            
            // 2. 对象前缀校验
            validateObjectPrefix(attachmentUrl);
            
            // 3. 文件扩展名和MIME类型校验
            validateFileType(attachmentUrl, attachmentName);
        }
    }
    
    /**
     * 验证OSS域名是否合法
     */
    private void validateOssDomain(String attachmentUrl) {
        Set<String> allowedDomains = attachmentProperties.allowedDomains();
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            // 如果没有配置允许的域名，跳过验证
            return;
        }
        
        try {
            URI uri = new URI(attachmentUrl);
            String host = uri.getHost();
            if (host == null) {
                throw new BusinessException(ErrorCode.ARTICLE_ATTACHMENT_INVALID);
            }
            
            boolean domainValid = allowedDomains.stream()
                    .anyMatch(domain -> host.equalsIgnoreCase(domain) || host.endsWith("." + domain));
            
            if (!domainValid) {
                throw new BusinessException(ErrorCode.ARTICLE_ATTACHMENT_INVALID);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ARTICLE_ATTACHMENT_INVALID);
        }
    }
    
    /**
     * 验证对象前缀是否在允许的目录下
     */
    private void validateObjectPrefix(String attachmentUrl) {
        String requiredPrefix = attachmentProperties.objectPrefix();
        if (requiredPrefix == null || requiredPrefix.isBlank()) {
            return;
        }
        
        try {
            URI uri = new URI(attachmentUrl);
            String path = uri.getPath();
            if (path == null) {
                throw new BusinessException(ErrorCode.ARTICLE_ATTACHMENT_INVALID);
            }
            
            // 移除开头的 /
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            if (!path.startsWith(requiredPrefix)) {
                throw new BusinessException(ErrorCode.ARTICLE_ATTACHMENT_INVALID);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ARTICLE_ATTACHMENT_INVALID);
        }
    }
    
    /**
     * 验证文件扩展名和MIME类型
     */
    private void validateFileType(String attachmentUrl, String attachmentName) {
        // 从文件名提取扩展名
        String extension = "";
        if (StringUtils.hasText(attachmentName)) {
            int lastDotIndex = attachmentName.lastIndexOf('.');
            if (lastDotIndex >= 0 && lastDotIndex < attachmentName.length() - 1) {
                extension = attachmentName.substring(lastDotIndex).toLowerCase();
            }
        }
        
        // 验证扩展名
        Set<String> allowedExtensions = attachmentProperties.allowedExtensions();
        if (allowedExtensions != null && !allowedExtensions.isEmpty()) {
            if (!allowedExtensions.contains(extension)) {
                throw new BusinessException(ErrorCode.ARTICLE_ATTACHMENT_INVALID);
            }
        }
        
        // 注意：MIME类型验证需要前端传递contentType参数
        // 当前DTO中没有contentType字段，可以在后续迭代中添加
    }
    
    /**
     * 验证状态转换合法性
     */
    private void validateStatusTransition(Integer currentStatus, Integer targetStatus) {
        // 定义合法的状态转换
        // DRAFT(0) -> PUBLISHED(1): 允许
        // PUBLISHED(1) -> OFFLINE(2): 允许
        // OFFLINE(2) -> PUBLISHED(1): 允许
        // PUBLISHED(1) -> DRAFT(0): 允许（撤回草稿）
        // 其他转换不允许
        
        boolean valid = false;
        if (currentStatus == 0 && targetStatus == 1) { // DRAFT -> PUBLISHED
            valid = true;
        } else if (currentStatus == 1 && targetStatus == 2) { // PUBLISHED -> OFFLINE
            valid = true;
        } else if (currentStatus == 2 && targetStatus == 1) { // OFFLINE -> PUBLISHED
            valid = true;
        } else if (currentStatus == 1 && targetStatus == 0) { // PUBLISHED -> DRAFT
            valid = true;
        }
        
        if (!valid) {
            throw new BusinessException(ErrorCode.ARTICLE_STATUS_INVALID);
        }
    }
    
    /**
     * 从完整URL中提取OSS ObjectKey
     */
    private String extractObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                return path.startsWith("/") ? path.substring(1) : path;
            }
        } catch (URISyntaxException ignored) {
        }
        String trimmed = url.startsWith("/") ? url.substring(1) : url;
        return trimmed;
    }
}
