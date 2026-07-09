package org.example.nursingtrainingbackend.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // ==================== 通用错误码 ====================
    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // ==================== 认证模块 (1xxx) ====================
    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USER_DISABLED(1002, "账号已被禁用"),

    // ==================== 文件上传模块 (2xxx) ====================
    FILE_EMPTY(2001, "上传文件不能为空"),
    FILE_TOO_LARGE(2002, "上传文件超过大小限制"),
    FILE_TYPE_NOT_ALLOWED(2003, "不支持的文件类型"),
    OSS_NOT_CONFIGURED(2004, "文件存储服务未配置"),
    FILE_UPLOAD_FAILED(2005, "文件上传失败"),

    // ==================== 类别管理模块 (3xxx) ====================
    CATEGORY_NOT_FOUND(3001, "类别不存在"),
    CATEGORY_NAME_EXISTS(3002, "类别名称已存在"),
    CATEGORY_HAS_CHILDREN(3003, "类别存在子类别，不能删除"),
    CATEGORY_HAS_COURSES(3004, "类别已关联课程，不能删除"),
    CATEGORY_PARENT_DISABLED(3005, "父类别已停用，请先启用父类别"),
    CATEGORY_HAS_ENABLED_CHILDREN(3006, "类别存在启用的子类别，不能停用"),
    CATEGORY_BATCH_DELETE_FAILED(3007, "批量删除失败，操作已全部回滚"),
    CATEGORY_INVALID_PARENT(3008, "父类别ID不合法，不能是自己或后代"),

    // ==================== 标签管理模块 (31xx) ====================
    TAG_NOT_FOUND(3101, "标签不存在"),
    TAG_NAME_EXISTS(3102, "标签名称已存在"),
    TAG_HAS_COURSES(3103, "标签已关联课程，不能删除"),
    TAG_BATCH_OPERATION_FAILED(3104, "批量操作失败，操作已全部回滚"),

    // ==================== 文章管理模块 (41xx) ====================
    ARTICLE_NOT_FOUND(4101, "文章不存在"),
    ARTICLE_TITLE_INVALID(4102, "文章标题不符合要求"),
    ARTICLE_CONTENT_EMPTY(4103, "文章正文不能为空"),
    ARTICLE_STATUS_INVALID(4104, "当前文章状态不允许该操作"),
    ARTICLE_ATTACHMENT_INVALID(4105, "附件信息不完整或文件类型不支持"),
    ARTICLE_NO_ATTACHMENT(4106, "文章没有附件，不能允许下载"),
    ARTICLE_BATCH_PUBLISH_FAILED(4107, "批量发布失败，操作已全部回滚"),
    ARTICLE_BATCH_DELETE_FAILED(4108, "批量删除失败，操作已全部回滚"),

    // ==================== 视频管理模块 (40xx) ====================
    VIDEO_NOT_FOUND(4001, "视频不存在"),
    VIDEO_FILE_TYPE_NOT_SUPPORTED(4002, "视频文件类型不支持"),
    VIDEO_FILE_SIZE_EXCEEDED(4003, "视频文件大小超过限制"),
    VIDEO_VOD_CREDENTIAL_FAILED(4004, "获取VOD上传凭证失败"),
    VIDEO_CREDENTIAL_EXPIRED(4005, "上传凭证已过期，请刷新凭证"),
    VIDEO_NOT_UPLOADED(4006, "视频尚未上传完成"),
    VIDEO_TRANSCODE_NOT_SUCCESS(4007, "视频转码未成功，不能发布"),
    VIDEO_TRANSCODE_RETRY_INVALID(4008, "当前转码状态不允许重试"),
    VIDEO_PLAY_AUTH_FAILED(4009, "获取播放凭证失败"),
    VIDEO_VOD_DELETE_FAILED(4010, "删除阿里云VOD媒资失败"),
    VIDEO_CALLBACK_SIGNATURE_INVALID(4011, "VOD回调签名无效"),
    VIDEO_STATUS_NO_CHANGE(4012, "视频业务状态无需重复修改"),
    VIDEO_BATCH_DELETE_FAILED(4013, "批量删除失败，操作已全部回滚"),
    VIDEO_TRANSCODE_SUBMIT_FAILED(4014, "提交转码任务失败"),

    // ==================== PPT 管理模块 (42xx) ====================
    PPT_NOT_FOUND(4201, "PPT 不存在"),
    PPT_STATUS_INVALID(4202, "当前状态不允许该操作"),
    PPT_HAS_COURSE(4203, "PPT 已关联课程，不能删除"),
    OSS_INVALID_URL(4204, "OSS 文件地址不合法"),
    OSS_FILE_NOT_FOUND(4205, "原始文件不存在");



    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
