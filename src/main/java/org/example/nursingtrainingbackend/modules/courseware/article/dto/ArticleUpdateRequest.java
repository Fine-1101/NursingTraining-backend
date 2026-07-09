package org.example.nursingtrainingbackend.modules.courseware.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章编辑请求DTO
 */
@Data
public class ArticleUpdateRequest {
    
    /**
     * 文章标题，长度1～200
     */
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题长度不能超过200")
    private String title;
    
    /**
     * 文章摘要，最长500
     */
    @Size(max = 500, message = "文章摘要长度不能超过500")
    private String summary;
    
    /**
     * HTML富文本正文
     */
    @NotBlank(message = "文章正文不能为空")
    private String content;
    
    /**
     * 封面图OSS地址
     */
    @Size(max = 500, message = "封面地址长度不能超过500")
    private String coverUrl;
    
    /**
     * 附件OSS地址；传null表示移除附件
     */
    @Size(max = 500, message = "附件地址长度不能超过500")
    private String attachmentUrl;
    
    /**
     * 附件原始文件名，有附件时必填
     */
    @Size(max = 255, message = "附件名称长度不能超过255")
    private String attachmentName;
    
    /**
     * 附件字节数，有附件时必填
     */
    private Long attachmentSize;
    
    /**
     * 是否允许下载附件
     */
    private Boolean allowDownload;
}
