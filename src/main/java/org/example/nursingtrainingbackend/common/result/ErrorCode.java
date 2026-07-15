package org.example.nursingtrainingbackend.common.result;

import lombok.Getter;
import org.springframework.http.HttpStatus;

//test
@Getter
public enum ErrorCode {
    // ==================== 通用错误码 ====================
    SUCCESS(0, "success", HttpStatus.OK),
    BAD_REQUEST(400, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "当前用户不是学员或无访问权限", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "资源不存在", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR(500, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),

    // ... existing code ...
    RATE_LIMIT_EXCEEDED(429, "请求过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS),
// ... existing code ...


    // ==================== 认证模块 (1xxx) ====================
    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误", HttpStatus.BAD_REQUEST),
    USER_DISABLED(1002, "账号已被禁用", HttpStatus.FORBIDDEN),
    USERNAME_EXISTS(1003, "用户名已存在", HttpStatus.BAD_REQUEST),
    INVALID_ROLE_TYPE(1004, "角色类型不合法", HttpStatus.BAD_REQUEST),
    DEPT_NOT_EXISTS(1005, "部门不存在", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1006, "用户不存在", HttpStatus.NOT_FOUND),

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
    COURSE_EXPORT_CONDITION_INVALID(5017, "导出条件不合法", HttpStatus.BAD_REQUEST),
    COURSE_EXCEL_GENERATE_FAILED(5018, "课程 Excel 生成失败", HttpStatus.INTERNAL_SERVER_ERROR),

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
    VIDEO_MP4_ONLY(4002, "仅支持 MP4 视频", HttpStatus.BAD_REQUEST),
    VIDEO_FILE_SIZE_EXCEEDED(4003, "视频文件超过大小限制", HttpStatus.BAD_REQUEST),
    VIDEO_OSS_INVALID(4004, "OSS 视频地址、对象路径或文件信息不合法", HttpStatus.BAD_REQUEST),
    VIDEO_STATUS_CONFLICT(4005, "当前视频状态不允许该操作", HttpStatus.CONFLICT),
    VIDEO_OSS_UPLOAD_FAILED(4006, "OSS 上传凭证或播放地址生成失败", HttpStatus.BAD_GATEWAY),
    VIDEO_IN_USE(4007, "视频已被课程点使用，不能删除", HttpStatus.CONFLICT),
    VIDEO_BATCH_DELETE_FAILED(4008, "批量删除失败，操作未全部完成", HttpStatus.CONFLICT),
    VIDEO_OSS_OBJECT_NOT_FOUND(4009, "OSS 视频对象不存在", HttpStatus.NOT_FOUND),

    // ==================== PPT 管理模块 (42xx) ====================

    PPT_NOT_FOUND(4201, "PPT 不存在", HttpStatus.NOT_FOUND),
    PPT_STATUS_INVALID(4202, "当前状态不允许该操作", HttpStatus.BAD_REQUEST),
    PPT_HAS_COURSE(4203, "PPT 已关联课程，不能删除", HttpStatus.CONFLICT),
    OSS_INVALID_URL(4204, "OSS 文件地址不合法", HttpStatus.BAD_REQUEST),
    OSS_FILE_NOT_FOUND(4205, "原始文件不存在", HttpStatus.NOT_FOUND),

    // ==================== 学员端模块 (6xxx) ====================
    LEARNER_NOT_FOUND(6001, "当前学员不存在或已停用", HttpStatus.NOT_FOUND),
    LEARNER_DEPT_NOT_BINDIED(6002, "当前学员未绑定部门，无法计算可学习课程", HttpStatus.BAD_REQUEST),
    LEARNER_HOME_QUERY_FAILED(6003, "首页数据查询失败", HttpStatus.INTERNAL_SERVER_ERROR),
    LEARNER_PAGE_PARAM_INVALID(6004, "分页参数不合法", HttpStatus.BAD_REQUEST),
    LEARNER_COURSE_NOT_VISIBLE(6005, "无权访问课程", HttpStatus.NOT_FOUND),
    LEARNER_COURSE_NOT_PUBLISHED(6006, "课程未发布或已下架", HttpStatus.BAD_REQUEST),
    LEARNER_POINT_NOT_FOUND(6007, "课程点不存在或已停用", HttpStatus.NOT_FOUND),
    LEARNER_VIDEO_PROGRESS_INVALID(6008, "视频进度参数不合法", HttpStatus.BAD_REQUEST),

    // ==================== 学习记录模块 (61xx) ====================
    LEARNER_RECORD_INVALID_ID(6101, "学习记录ID格式不合法", HttpStatus.BAD_REQUEST),
    LEARNER_RECORD_NOT_FOUND(6102, "学习记录不存在", HttpStatus.NOT_FOUND),
    LEARNER_RECORD_ALREADY_COMPLETED(6103, "该课件已标记为完成", HttpStatus.CONFLICT),
    LEARNER_RECORD_TYPE_NOT_SUPPORTED(6104, "该记录类型不支持此操作", HttpStatus.BAD_REQUEST),

    // ==================== 学习记录参数校验模块 (64xx) ====================
    LEARNER_RECORD_RANGE_INVALID(6401, "时间范围参数不合法", HttpStatus.BAD_REQUEST),
    LEARNER_RECORD_TYPE_PARAM_INVALID(6402, "行为类型参数不合法", HttpStatus.BAD_REQUEST),
    LEARNER_RECORD_RESOURCE_TYPE_INVALID(6403, "课件类型参数不合法", HttpStatus.BAD_REQUEST),
    LEARNER_RECORD_PAGE_PARAM_INVALID(6404, "分页参数不合法", HttpStatus.BAD_REQUEST),
    LEARNER_RECORD_QUERY_FAILED(6405, "学习记录查询失败", HttpStatus.INTERNAL_SERVER_ERROR),
    LEARNER_RECORD_STATS_FAILED(6406, "学习记录统计失败", HttpStatus.INTERNAL_SERVER_ERROR),

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
    STUDENT_PROGRESS_UPDATE_FAILED(7113, "学员课程进度更新失败", HttpStatus.CONFLICT),

    //管理端首页面板错误码
    DASHBOARD_PANEL_INVALID_ID(7001, "面板ID格式不合法", HttpStatus.BAD_REQUEST),
    DASHBOARD_PANEL_NOT_FOUND(7002, "面板统计失败", HttpStatus.INTERNAL_SERVER_ERROR),
    DASHBOARD_PANEL_QUERY_FAILED(7003, "面板查询失败", HttpStatus.INTERNAL_SERVER_ERROR),

    // ==================== 考核模块 (80xx) ====================
    QUESTION_NOT_FOUND(8001, "题目不存在", HttpStatus.NOT_FOUND),
    QUESTION_TYPE_NOT_SUPPORTED(8002, "题型不支持", HttpStatus.BAD_REQUEST),
    QUESTION_OPTION_INVALID(8003, "选项或正确答案不合法", HttpStatus.BAD_REQUEST),
    QUESTION_COURSE_CATEGORY_MISMATCH(8004, "指定课程与题目类别不一致", HttpStatus.BAD_REQUEST),
    ASSESSMENT_NOT_FOUND(8010, "考核不存在", HttpStatus.NOT_FOUND),
    ASSESSMENT_STATUS_CONFLICT(8011, "考核状态不允许当前操作", HttpStatus.CONFLICT),
    ASSESSMENT_DRAW_RULE_INVALID(8012, "抽题规则不合法", HttpStatus.BAD_REQUEST),
    ASSESSMENT_QUESTION_INSUFFICIENT(8013, "可用题量不足", HttpStatus.CONFLICT),
    ASSESSMENT_SCORE_INVALID(8014, "总分或及格分不合法", HttpStatus.BAD_REQUEST),
    ASSESSMENT_TIME_INVALID(8015, "考核时间设置不合法", HttpStatus.BAD_REQUEST),
    ASSESSMENT_FORBIDDEN(8020, "无权参加考核", HttpStatus.FORBIDDEN),
    ASSESSMENT_NOT_STARTED(8021, "尚未到开考时间", HttpStatus.CONFLICT),
    ASSESSMENT_CLOSED(8022, "考核已关闭", HttpStatus.CONFLICT),
    ASSESSMENT_ATTEMPTS_EXHAUSTED(8023, "次数已用完", HttpStatus.CONFLICT),
    ASSESSMENT_ATTEMPT_NOT_FOUND(8024, "考试记录不存在", HttpStatus.NOT_FOUND),
    ASSESSMENT_ATTEMPT_NOT_OWNER(8025, "记录不属于当前学员", HttpStatus.FORBIDDEN),
    ASSESSMENT_ALREADY_SUBMITTED(8026, "已经交卷", HttpStatus.CONFLICT),
    ASSESSMENT_OPTION_MISMATCH(8027, "选项不属于当前题目", HttpStatus.BAD_REQUEST),
    ASSESSMENT_AUTO_SUBMITTED(8028, "已超时并自动交卷", HttpStatus.CONFLICT),
    ASSESSMENT_DRAW_FAILED(8029, "随机组卷失败", HttpStatus.INTERNAL_SERVER_ERROR),
    ASSESSMENT_GRADE_FAILED(8030, "自动判卷失败", HttpStatus.INTERNAL_SERVER_ERROR),

    LEARNING_REPORT_NOT_FOUND(
        6501, "学习报告不存在", HttpStatus.NOT_FOUND),
    LEARNING_REPORT_TYPE_INVALID(
        6502, "学习报告类型不合法", HttpStatus.BAD_REQUEST),
    LEARNING_REPORT_PERIOD_INVALID(
        6503, "报告统计周期不合法", HttpStatus.BAD_REQUEST),
    LEARNING_REPORT_GENERATING(
        6504, "相同周期报告正在生成", HttpStatus.CONFLICT),
    LEARNING_REPORT_RATE_LIMITED(
        6505, "报告生成次数超过限制", HttpStatus.TOO_MANY_REQUESTS),
    LEARNING_REPORT_DATA_UNCHANGED(
        6506, "学习数据未变化，无需重新生成", HttpStatus.CONFLICT),
    LEARNING_REPORT_COURSE_REQUIRED(
        6507, "单课程报告缺少课程ID", HttpStatus.BAD_REQUEST),
    LEARNING_REPORT_COURSE_UNAVAILABLE(
        6508, "课程不存在或当前用户不可访问", HttpStatus.NOT_FOUND),
    AI_PROVIDER_UNAVAILABLE(
        6509, "AI服务暂不可用", HttpStatus.SERVICE_UNAVAILABLE),
    AI_PROVIDER_TIMEOUT(
        6510, "AI报告生成超时", HttpStatus.GATEWAY_TIMEOUT),
    AI_RESPONSE_INVALID(
        6511, "AI报告格式校验失败", HttpStatus.INTERNAL_SERVER_ERROR),
    LEARNING_REPORT_FEEDBACK_INVALID(
            6512, "学习报告反馈内容不合法", HttpStatus.BAD_REQUEST),


    // ==================== 消息通知模块 (90xx) ====================
    MESSAGE_NOT_FOUND(9001, "消息不存在", HttpStatus.NOT_FOUND),
    MESSAGE_CONTENT_INVALID(9002, "消息内容长度必须为1到1000个字符", HttpStatus.BAD_REQUEST),
    MESSAGE_SEND_FORBIDDEN(9003, "无权发送学员消息", HttpStatus.FORBIDDEN),
    WS_TICKET_RATE_LIMITED(9004, "申请过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
