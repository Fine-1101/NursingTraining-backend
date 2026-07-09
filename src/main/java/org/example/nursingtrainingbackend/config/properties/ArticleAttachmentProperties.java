package org.example.nursingtrainingbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Set;

/**
 * 文章附件上传配置
 */
@ConfigurationProperties(prefix = "app.article.attachment")
public record ArticleAttachmentProperties(
        /**
         * 允许的OSS域名列表
         */
        Set<String> allowedDomains,
        
        /**
         * 对象前缀（如 articles/attachments/）
         */
        String objectPrefix,
        
        /**
         * 允许的文件扩展名（小写，如 .pdf, .doc, .docx）
         */
        Set<String> allowedExtensions,
        
        /**
         * 允许的MIME类型
         */
        Set<String> allowedMimeTypes
) {
    public ArticleAttachmentProperties {
        if (objectPrefix == null) objectPrefix = "files/articles/attachments/";
        if (allowedDomains == null) allowedDomains = Set.of();
        if (allowedExtensions == null) allowedExtensions = Set.of(".pdf", ".doc", ".docx");
        if (allowedMimeTypes == null) allowedMimeTypes = Set.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
    }
}
