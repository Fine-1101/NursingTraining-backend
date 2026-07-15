package org.example.nursingtrainingbackend.modules.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.message.dto.MessageSendRequest;
import org.example.nursingtrainingbackend.modules.message.entity.CourseStudentMessage;
import org.example.nursingtrainingbackend.modules.message.mapper.CourseStudentMessageMapper;
import org.example.nursingtrainingbackend.modules.message.vo.MarkAllReadVO;
import org.example.nursingtrainingbackend.modules.message.vo.MarkReadVO;
import org.example.nursingtrainingbackend.modules.message.vo.MessageItemVO;
import org.example.nursingtrainingbackend.modules.message.vo.MessageSendVO;
import org.example.nursingtrainingbackend.modules.message.vo.UnreadCountVO;
import org.example.nursingtrainingbackend.modules.message.websocket.NotificationWebSocketHandler;
import org.example.nursingtrainingbackend.modules.system.mapper.SettingsStudentMapper;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseStudentMessageService {

    private final CourseStudentMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final SettingsStudentMapper settingsStudentMapper;
    private final NotificationWebSocketHandler webSocketHandler;

    @Transactional
    public CourseStudentMessage sendCourseMessage(Long studentId, Long courseId, MessageSendRequest request) {
        // 1. 校验管理员身份与角色
        User admin = getAdminOrThrow(SecurityUtils.currentUserId());

        // 2. 查询学员，校验状态和角色
        User student = getStudentOrThrow(studentId);

        // 3. 查询课程并获取课程名称
        Course course = getCourseOrThrow(courseId);

        // 4. 校验学员与课程的关联关系
        validateCourseForStudent(courseId, student.getDeptId());

        // 5. 清理并校验消息内容
        String content = request.content().trim();
        if (content.isEmpty() || content.length() > 1000) {
            throw new BusinessException(ErrorCode.MESSAGE_CONTENT_INVALID);
        }

        // 6. 插入消息记录
        CourseStudentMessage message = new CourseStudentMessage();
        message.setReceiverId(studentId);
        message.setSenderId(admin.getId());
        message.setCourseId(courseId);
        message.setCourseTitle(course.getTitle());
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        // 7. 注册事务提交后回调，推送 WebSocket
        final Long senderId = admin.getId();
        final String senderName = admin.getRealName();
        final String courseTitle = course.getTitle();
        final LocalDateTime createdAt = message.getCreatedAt();
        final Long msgId = message.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 8. 事务提交成功后推送
                int sent = pushWebSocket(studentId, msgId, courseId, courseTitle,
                        content, senderName, createdAt);
                // 9. 记录日志（不含完整消息正文）
                log.info("发送课程消息完成: messageId={}, senderId={}, receiverId={}, courseId={}, wsSent={}",
                        msgId, senderId, studentId, courseId, sent);
            }
        });

        return message;
    }

    public MessageSendVO buildSendVO(CourseStudentMessage message, Long studentId) {
        User student = userMapper.selectById(studentId);

        MessageSendVO vo = new MessageSendVO();
        vo.setMessageId(message.getId());
        vo.setStudentId(studentId);
        vo.setStudentName(student != null ? student.getRealName() : null);
        vo.setCourseId(message.getCourseId());
        vo.setCourseTitle(message.getCourseTitle());
        vo.setContent(message.getContent());
        vo.setCreatedAt(toOffset(message.getCreatedAt()));
        return vo;
    }

    public PageResult<MessageItemVO> selectMessagePage(int page, int size, String readStatus) {
        Long userId = SecurityUtils.currentUserId();

        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;

        String status = (readStatus == null || readStatus.isBlank()) ? "ALL" : readStatus.trim().toUpperCase();
        if (!status.equals("ALL") && !status.equals("UNREAD") && !status.equals("READ")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "readStatus 参数不合法");
        }

        Page<CourseStudentMessage> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<CourseStudentMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseStudentMessage::getReceiverId, userId);

        switch (status) {
            case "UNREAD" -> wrapper.isNull(CourseStudentMessage::getReadAt);
            case "READ" -> wrapper.isNotNull(CourseStudentMessage::getReadAt);
        }

        wrapper.orderByDesc(CourseStudentMessage::getCreatedAt)
                .orderByDesc(CourseStudentMessage::getId);

        Page<CourseStudentMessage> result = messageMapper.selectPage(pageObj, wrapper);

        Set<Long> senderIds = result.getRecords().stream()
                .map(CourseStudentMessage::getSenderId)
                .collect(Collectors.toSet());
        Map<Long, String> senderNameMap = Map.of();
        if (!senderIds.isEmpty()) {
            senderNameMap = userMapper.selectBatchIds(senderIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : "", (a, b) -> a));
        }

        Map<Long, String> finalSenderNameMap = senderNameMap;
        java.util.List<MessageItemVO> voList = result.getRecords().stream().map(msg -> {
            MessageItemVO vo = new MessageItemVO();
            vo.setMessageId(msg.getId());
            vo.setCourseId(msg.getCourseId());
            vo.setCourseTitle(msg.getCourseTitle());
            vo.setContent(msg.getContent());
            vo.setSenderName(finalSenderNameMap.getOrDefault(msg.getSenderId(), ""));
            vo.setRead(msg.getReadAt() != null);
            vo.setReadAt(toOffset(msg.getReadAt()));
            vo.setCreatedAt(toOffset(msg.getCreatedAt()));
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }

    public UnreadCountVO getUnreadCount() {
        Long userId = SecurityUtils.currentUserId();
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<CourseStudentMessage>()
                        .eq(CourseStudentMessage::getReceiverId, userId)
                        .isNull(CourseStudentMessage::getReadAt));
        UnreadCountVO vo = new UnreadCountVO();
        vo.setUnreadCount(count);
        return vo;
    }

    @Transactional
    public MarkReadVO markAsRead(Long messageId) {
        Long userId = SecurityUtils.currentUserId();
        messageMapper.markAsRead(messageId, userId);

        CourseStudentMessage message = messageMapper.selectById(messageId);
        if (message == null || !message.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        MarkReadVO vo = new MarkReadVO();
        vo.setMessageId(messageId);
        vo.setRead(true);
        vo.setReadAt(toOffset(message.getReadAt()));
        return vo;
    }

    @Transactional
    public MarkAllReadVO readAllMessages() {
        Long userId = SecurityUtils.currentUserId();
        int updated = messageMapper.markAllAsRead(userId);

        MarkAllReadVO vo = new MarkAllReadVO();
        vo.setUpdatedCount((long) updated);
        vo.setReadAt(toOffset(LocalDateTime.now()));
        return vo;
    }

    // ==================== 私有方法 ====================

    private User getAdminOrThrow(Long adminUserId) {
        User admin = userMapper.selectById(adminUserId);
        if (admin == null || admin.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        if (admin.getRoleType() == null || admin.getRoleType() == 1) {
            throw new BusinessException(ErrorCode.MESSAGE_SEND_FORBIDDEN);
        }
        return admin;
    }

    private User getStudentOrThrow(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new BusinessException(ErrorCode.STUDENT_ID_INVALID);
        }
        User student = userMapper.selectById(studentId);
        if (student == null || student.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }
        if (student.getRoleType() == null || student.getRoleType() != 1) {
            throw new BusinessException(ErrorCode.NOT_STUDENT_ROLE);
        }
        return student;
    }

    private Course getCourseOrThrow(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    private void validateCourseForStudent(Long courseId, Long deptId) {
        Integer courseStatus = settingsStudentMapper.selectPublishedCourseInDept(courseId, deptId);
        if (courseStatus == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_AVAILABLE_FOR_STUDENT);
        }
    }

    private int pushWebSocket(Long studentId, Long messageId, Long courseId,
                              String courseTitle, String content,
                              String senderName, LocalDateTime createdAt) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("messageId", messageId);
            data.put("courseId", courseId);
            data.put("courseTitle", courseTitle);
            data.put("content", content);
            data.put("senderName", senderName);
            data.put("read", false);
            data.put("createdAt", toOffset(createdAt).toString());

            Map<String, Object> envelope = new HashMap<>();
            envelope.put("event", "COURSE_STUDENT_MESSAGE_CREATED");
            envelope.put("eventId", "message:" + messageId);
            envelope.put("occurredAt", toOffset(createdAt).toString());
            envelope.put("data", data);

            return webSocketHandler.sendToUser(studentId, envelope);
        } catch (Exception e) {
            log.warn("推送WebSocket消息异常: studentId={}, messageId={}", studentId, messageId, e);
            return 0;
        }
    }

    private OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt != null ? ldt.atOffset(ZoneOffset.ofHours(8)) : null;
    }
}
