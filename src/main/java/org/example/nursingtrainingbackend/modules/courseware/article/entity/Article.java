package org.example.nursingtrainingbackend.modules.courseware.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培训文章实体类
 */
@Data
@TableName("article")
public class Article {

    /**
     * 文章ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章正文（HTML富文本）
     */
    private String content;

    /**
     * 封面图URL
     */
    private String coverUrl;

    /**
     * 文章附件OSS地址
     */
    private String attachmentUrl;

    /**
     * 附件原始文件名
     */
    private String attachmentName;

    /**
     * 附件字节数
     */
    private Long attachmentSize;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 阅读完成人数
     */
    private Integer readCount;

    /**
     * 是否允许下载附件：0-否 1-是
     */
    private Integer allowDownload;

    /**
     * 状态：0-草稿 1-已发布 2-已下架
     */
    private Integer status;

    /**
     * 最近一次发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    private LocalDateTime deletedAt;
}
