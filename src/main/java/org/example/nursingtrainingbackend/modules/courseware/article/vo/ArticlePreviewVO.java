package org.example.nursingtrainingbackend.modules.courseware.article.vo;

import lombok.Data;

/**
 * 文章预览VO
 */
@Data
public class ArticlePreviewVO {
    
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
     * 清洗后的HTML正文
     */
    private String content;
    
    /**
     * 封面地址
     */
    private String coverUrl;
    
    /**
     * 上传者姓名
     */
    private String uploaderName;
    
    /**
     * 当前状态
     */
    private String status;
    
    /**
     * 是否允许下载附件
     */
    private Boolean allowDownload;
    
    /**
     * 附件名称；预览接口不直接返回附件下载地址
     */
    private String attachmentName;
}
