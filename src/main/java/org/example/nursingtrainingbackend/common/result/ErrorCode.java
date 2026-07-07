package org.example.nursingtrainingbackend.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USER_DISABLED(1002, "账号已被禁用"),
    FILE_EMPTY(2001, "上传文件不能为空"),
    FILE_TOO_LARGE(2002, "上传文件超过大小限制"),
    FILE_TYPE_NOT_ALLOWED(2003, "不支持的文件类型"),
    OSS_NOT_CONFIGURED(2004, "文件存储服务未配置"),
    FILE_UPLOAD_FAILED(2005, "文件上传失败"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
