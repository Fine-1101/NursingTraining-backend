package org.example.nursingtrainingbackend.modules.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件上传记录实体
 */
@Data
@TableName("file_upload_record")
public class FileUploadRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** OSS对象Key */
    private String objectKey;

    /** 文件访问URL */
    private String url;

    /** 上传类型：DIRECT-直传, POLICY-策略上传 */
    private String uploadType;

    /** 原始文件名 */
    private String originalFileName;

    /** 文件大小（字节） */
    private Long size;

    /** MIME类型 */
    private String contentType;

    /** 业务类型：COURSE_COVER, VIDEO, ARTICLE, PPT, AVATAR等 */
    private String bizType;

    /** 业务数据ID */
    private Long bizId;

    /** 是否已绑定业务：0-未使用，1-已使用 */
    private Integer used;

    /** 上传人ID */
    private Long createdBy;

    /** 上传时间 */
    private LocalDateTime createdAt;

    /** 绑定业务时间 */
    private LocalDateTime usedAt;

    /** 软删除时间 */
    private LocalDateTime deletedAt;
}
