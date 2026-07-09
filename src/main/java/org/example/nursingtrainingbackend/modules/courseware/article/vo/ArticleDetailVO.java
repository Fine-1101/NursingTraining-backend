package org.example.nursingtrainingbackend.modules.courseware.article.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章详情VO
 */
@Data
public class ArticleDetailVO {
    
    /**
     * 文章ID
     */
    private Long id;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 摘要
     */
    private String summary;
    
    /**
     * HTML富文本正文
     */
    private String content;
    
    /**
     * OSS封面地址
     */
    private String coverUrl;
    
    /**
     * OSS附件地址
     */
    private String attachmentUrl;
    
    /**
     * 附件原始名称
     */
    private String attachmentName;
    
    /**
     * 附件字节数
     */
    private Long attachmentSize;
    
    /**
     * 是否允许下载附件
     */
    private Boolean allowDownload;
    
    /**
     * 上传者ID
     */
    private Long uploaderId;
    
    /**
     * 上传者姓名
     */
    private String uploaderName;
    
    /**
     * 浏览量
     */
    private Integer viewCount;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadedAt;
    
    /**
     * 最近发布时间
     */
    private LocalDateTime publishedAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
