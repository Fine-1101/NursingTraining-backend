package org.example.nursingtrainingbackend.common.result;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // ==================== 通用错误码 ====================
    SUCCESS(0, "success", HttpStatus.OK),
    BAD_REQUEST(400, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "无权访问", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "资源不存在", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR(500, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),

    // ==================== 认证模块 (1xxx) ====================
    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误", HttpStatus.BAD_REQUEST),
    USER_DISABLED(1002, "账号已被禁用", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND(1003, "用户不存在", HttpStatus.NOT_FOUND),
    USERNAME_EXISTS(1004, "用户名已存在", HttpStatus.BAD_REQUEST),

    // ==================== 文件上传模块 (2xxx) ====================
    FILE_EMPTY(2001, "上传文件不能为空", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(2002, "上传文件超过大小限制", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_TYPE_NOT_ALLOWED(2003, "不支持的文件类型", HttpStatus.BAD_REQUEST),
    OSS_NOT_CONFIGURED(2004, "文件存储服务未配置", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_UPLOAD_FAILED(2005, "文件上传失败", HttpStatus.INTERNAL_SERVER_ERROR),

    // ==================== 类别管理模块 (3xxx) ====================
    CATEGORY_NOT_FOUND(3001, "类别不存在", HttpStatus.NOT_FOUND),
    CATEGORY_NAME_EXISTS(3002, "类别名称已存在", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_CHILDREN(3003, "类别存在子类别，不能删除", HttpStatus.CONFLICT),
    CATEGORY_HAS_COURSES(3004, "类别已关联课程，不能删除", HttpStatus.CONFLICT),
    CATEGORY_PARENT_DISABLED(3005, "父类别已停用，请先启用父类别", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_ENABLED_CHILDREN(3006, "类别存在启用的子类别，不能停用", HttpStatus.CONFLICT),
    CATEGORY_BATCH_DELETE_FAILED(3007, "批量删除失败，操作已全部回滚", HttpStatus.INTERNAL_SERVER_ERROR),
    CATEGORY_INVALID_PARENT(3008, "父类别ID不合法，不能是自己或后代", HttpStatus.BAD_REQUEST),
    CATEGORY_ANCESTOR_DISABLED(3009, "父类别已停用，请先启用父类别", HttpStatus.CONFLICT),
    CATEGORY_HAS_ENABLED_DESCENDANTS(3010, "存在启用的后代类别，不能停用", HttpStatus.CONFLICT),

    // ==================== 标签管理模块 (31xx) ====================
    TAG_NOT_FOUND(3101, "标签不存在", HttpStatus.NOT_FOUND),
    TAG_NAME_EXISTS(3102, "标签名称已存在", HttpStatus.BAD_REQUEST),
    TAG_HAS_COURSES(3103, "标签已关联课程，不能删除", HttpStatus.CONFLICT),
    TAG_BATCH_OPERATION_FAILED(3104, "批量操作失败，操作已全部回滚", HttpStatus.INTERNAL_SERVER_ERROR),





    // ==================== 课程管理模块 (50xx) ====================
    COURSE_NOT_FOUND(5001, "课程不存在", HttpStatus.NOT_FOUND),
    COURSE_BASIC_INFO_INCOMPLETE(5002, "课程基础信息不完整", HttpStatus.BAD_REQUEST),
    COURSE_TAG_LIMIT(5003, "标签最多选择 3 个且不能重复", HttpStatus.BAD_REQUEST),
    COURSE_TAG_INVALID(5004, "标签不存在、已停用或已删除", HttpStatus.BAD_REQUEST),
    COURSE_INSTRUCTOR_INVALID(5005, "讲师不存在、已停用或不是讲师角色", HttpStatus.BAD_REQUEST),
    COURSE_DEPARTMENT_INVALID(5006, "部门列表不能为空、重复或包含停用部门", HttpStatus.BAD_REQUEST),
    COURSE_CHAPTER_NOT_FOUND(5007, "章节不存在或不属于当前课程", HttpStatus.NOT_FOUND),
    COURSE_CHAPTER_HAS_POINTS(5008, "章节下存在课程点，不能删除", HttpStatus.CONFLICT),
    COURSE_POINT_NOT_FOUND(5009, "课程点不存在或不属于当前章节", HttpStatus.NOT_FOUND),
    COURSE_POINT_NO_MEDIA(5010, "课程点至少关联一个有效课件", HttpStatus.BAD_REQUEST),
    COURSE_MEDIA_INVALID(5011, "关联课件不存在、未发布或已删除", HttpStatus.BAD_REQUEST),
    COURSE_SORT_DATA_INCOMPLETE(5012, "章节或课程点顺序数据不完整", HttpStatus.BAD_REQUEST),
    COURSE_STRUCTURE_NOT_PUBLISHABLE(5013, "课程结构不满足发布条件", HttpStatus.CONFLICT),
    COURSE_STATUS_INVALID(5014, "当前课程状态不允许该操作", HttpStatus.CONFLICT),
    COURSE_COVER_URL_INVALID(5015, "课程封面 OSS 地址不合法", HttpStatus.BAD_REQUEST),
    COURSE_NOT_DRAFT(5016, "课程已发布，不能按草稿规则删除", HttpStatus.CONFLICT),

    // ==================== 文章管理模块 (41xx) ====================
    ARTICLE_NOT_FOUND(4101, "文章不存在", HttpStatus.NOT_FOUND),
    ARTICLE_TITLE_INVALID(4102, "文章标题不符合要求", HttpStatus.BAD_REQUEST),
    ARTICLE_CONTENT_EMPTY(4103, "文章正文不能为空", HttpStatus.BAD_REQUEST),
    ARTICLE_STATUS_INVALID(4104, "当前文章状态不允许该操作", HttpStatus.BAD_REQUEST),
    ARTICLE_ATTACHMENT_INVALID(4105, "附件信息不完整或文件类型不支持", HttpStatus.BAD_REQUEST),
    ARTICLE_NO_ATTACHMENT(4106, "文章没有附件，不能允许下载", HttpStatus.BAD_REQUEST),
    ARTICLE_BATCH_PUBLISH_FAILED(4107, "批量发布失败，操作已全部回滚", HttpStatus.INTERNAL_SERVER_ERROR),
    ARTICLE_BATCH_DELETE_FAILED(4108, "批量删除失败，操作已全部回滚", HttpStatus.INTERNAL_SERVER_ERROR),

    // ==================== 视频管理模块 (40xx) ====================
    VIDEO_NOT_FOUND(4001, "视频不存在", HttpStatus.NOT_FOUND),
    VIDEO_FILE_TYPE_NOT_SUPPORTED(4002, "视频文件类型不支持", HttpStatus.BAD_REQUEST),
    VIDEO_FILE_SIZE_EXCEEDED(4003, "视频文件大小超过限制", HttpStatus.BAD_REQUEST),
    VIDEO_VOD_CREDENTIAL_FAILED(4004, "获取VOD上传凭证失败", HttpStatus.INTERNAL_SERVER_ERROR),
    VIDEO_CREDENTIAL_EXPIRED(4005, "上传凭证已过期，请刷新凭证", HttpStatus.BAD_REQUEST),
    VIDEO_NOT_UPLOADED(4006, "视频尚未上传完成", HttpStatus.BAD_REQUEST),
    VIDEO_TRANSCODE_NOT_SUCCESS(4007, "视频转码未成功，不能发布", HttpStatus.BAD_REQUEST),
    VIDEO_TRANSCODE_RETRY_INVALID(4008, "当前转码状态不允许重试", HttpStatus.BAD_REQUEST),
    VIDEO_PLAY_AUTH_FAILED(4009, "获取播放凭证失败", HttpStatus.INTERNAL_SERVER_ERROR),
    VIDEO_VOD_DELETE_FAILED(4010, "删除阿里云VOD媒资失败", HttpStatus.INTERNAL_SERVER_ERROR),
    VIDEO_CALLBACK_SIGNATURE_INVALID(4011, "VOD回调签名无效", HttpStatus.BAD_REQUEST),
    VIDEO_STATUS_NO_CHANGE(4012, "视频业务状态无需重复修改", HttpStatus.BAD_REQUEST),
    VIDEO_BATCH_DELETE_FAILED(4013, "批量删除失败，操作已全部回滚", HttpStatus.INTERNAL_SERVER_ERROR),
    VIDEO_TRANSCODE_SUBMIT_FAILED(4014, "提交转码任务失败", HttpStatus.INTERNAL_SERVER_ERROR),

    // ==================== PPT 管理模块 (42xx) ====================

    PPT_NOT_FOUND(4201, "PPT 不存在", HttpStatus.NOT_FOUND),
    PPT_STATUS_INVALID(4202, "当前状态不允许该操作", HttpStatus.BAD_REQUEST),
    PPT_HAS_COURSE(4203, "PPT 已关联课程，不能删除", HttpStatus.CONFLICT),
    OSS_INVALID_URL(4204, "OSS 文件地址不合法", HttpStatus.BAD_REQUEST),
    OSS_FILE_NOT_FOUND(4205, "原始文件不存在", HttpStatus.NOT_FOUND),

    // ==================== 系统设置/学员管理模块 (71xx) ====================
    ADMIN_NOT_FOUND(7101, "当前管理员不存在或已停用", HttpStatus.NOT_FOUND),
    STUDENT_NOT_FOUND(7102, "学员不存在或已删除", HttpStatus.NOT_FOUND),
    STUDENT_ID_INVALID(7103, "学员 ID 不合法", HttpStatus.BAD_REQUEST),
    PHONE_FORMAT_INVALID(7104, "手机号格式不合法", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NOT_FOUND_OR_DISABLED(7105, "科室不存在或已停用", HttpStatus.BAD_REQUEST),
    NOT_STUDENT_ROLE(7106, "当前用户不是学员，不能执行该操作", HttpStatus.CONFLICT),
    STUDENT_USERNAME_EXISTS(7107, "工号已存在", HttpStatus.CONFLICT),
    STUDENT_QUERY_FAILED(7108, "学员查询失败", HttpStatus.INTERNAL_SERVER_ERROR),
    STUDENT_SAVE_FAILED(7109, "学员保存失败", HttpStatus.INTERNAL_SERVER_ERROR),
    STUDENT_DELETE_FAILED(7110, "学员删除失败", HttpStatus.INTERNAL_SERVER_ERROR),
    AVATAR_URL_OR_KEY_INVALID(7111, "头像地址或 OSS Key 不合法", HttpStatus.BAD_REQUEST),
    COURSE_NOT_AVAILABLE_FOR_STUDENT(7112, "课程不存在、未发布或该学员不可学习", HttpStatus.NOT_FOUND),
    STUDENT_PROGRESS_UPDATE_FAILED(7113, "学员课程进度更新失败", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}