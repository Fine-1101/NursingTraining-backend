package org.example.nursingtrainingbackend.modules.assessment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.assessment.dto.ParticipantQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.dto.ReminderQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.dto.ReminderSendRequest;
import org.example.nursingtrainingbackend.modules.assessment.entity.Assessment;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentAttemptMapper;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentMapper;
import org.example.nursingtrainingbackend.modules.assessment.service.AssessmentParticipantService;
import org.example.nursingtrainingbackend.modules.assessment.vo.ParticipantItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ReminderItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ReminderSendResultVO;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.message.entity.CourseStudentMessage;
import org.example.nursingtrainingbackend.modules.message.mapper.CourseStudentMessageMapper;
import org.example.nursingtrainingbackend.modules.message.websocket.NotificationWebSocketHandler;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentParticipantServiceImpl implements AssessmentParticipantService {

    private final AssessmentMapper assessmentMapper;
    private final AssessmentAttemptMapper attemptMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final CourseStudentMessageMapper messageMapper;
    private final NotificationWebSocketHandler webSocketHandler;

    private static final Set<String> VALID_STATUSES = Set.of(
            "ALL", "PARTICIPATED", "NOT_PARTICIPATED", "IN_PROGRESS", "SUBMITTED");

    private static final DateTimeFormatter BATCH_DT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 管理员维度频率限制：assessmentId-adminId -> lastSendTime */
    private final ConcurrentHashMap<String, LocalDateTime> rateLimitMap = new ConcurrentHashMap<>();

    // ==================== 查询考核参与人员 ====================

    @Override
    public PageResult<ParticipantItemVO> listParticipants(Long assessmentId, ParticipantQueryDTO query) {
        Assessment assessment = getAssessmentOrThrow(assessmentId);

        // 校验 participationStatus
        String status = query.getParticipationStatus();
        if (status == null || status.isBlank()) {
            status = "ALL";
        }
        status = status.trim().toUpperCase();
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.ASSESSMENT_PARTICIPATION_STATUS_INVALID);
        }

        Page<ParticipantItemVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<ParticipantItemVO> result = attemptMapper.selectParticipants(
                page,
                assessmentId,
                assessment.getCourseId(),
                assessment.getCategoryId(),
                "ALL".equals(status) ? null : status,
                query.getKeyword(),
                query.getDepartmentId()
        );

        return new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }

    // ==================== 提醒未参加考核人员 ====================

    @Override
    @Transactional
    public ReminderSendResultVO sendReminders(Long assessmentId, ReminderSendRequest request) {
        // 1. 校验考核状态
        Assessment assessment = getAssessmentOrThrow(assessmentId);
        if (assessment.getStatus() == null || assessment.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ASSESSMENT_REMINDER_NOT_ALLOWED);
        }
        if (assessment.getEndAt() != null && LocalDateTime.now().isAfter(assessment.getEndAt())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_REMINDER_NOT_ALLOWED, "已超过最晚开考时间，不能发送提醒");
        }

        // 2. 获取课程信息
        Course course = courseMapper.selectById(assessment.getCourseId());
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        // 3. 频率限制（管理员 + 考核维度，60秒内不能重复批量发送）
        Long adminId = SecurityUtils.currentUserId();
        String rateLimitKey = assessmentId + "-" + adminId;
        LocalDateTime lastSend = rateLimitMap.get(rateLimitKey);
        if (lastSend != null && lastSend.plusSeconds(60).isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_REMINDER_RATE_LIMITED);
        }

        // 4. 确定要提醒的用户ID列表
        List<Long> targetUserIds;
        if (Boolean.TRUE.equals(request.getRemindAll())) {
            // 查询全部未参加人员
            targetUserIds = attemptMapper.selectNonParticipantUserIds(
                    assessmentId, assessment.getCourseId(), assessment.getCategoryId());
        } else {
            List<Long> userIds = request.getUserIds();
            if (userIds == null || userIds.isEmpty()) {
                throw new BusinessException(ErrorCode.ASSESSMENT_REMINDER_USER_IDS_REQUIRED);
            }
            if (userIds.size() > 100) {
                throw new BusinessException(ErrorCode.ASSESSMENT_REMINDER_USER_IDS_TOO_MANY);
            }
            // 校验去重
            if (new HashSet<>(userIds).size() != userIds.size()) {
                throw new BusinessException(ErrorCode.ASSESSMENT_REMINDER_USER_IDS_DUPLICATE);
            }
            targetUserIds = new ArrayList<>(userIds);
        }

        int requestedCount = targetUserIds.size();
        if (requestedCount == 0) {
            // 没有需要提醒的人
            ReminderSendResultVO result = new ReminderSendResultVO();
            result.setAssessmentId(assessmentId);
            result.setCourseId(assessment.getCourseId());
            result.setRequestedCount(0);
            result.setSentCount(0);
            result.setSkippedCount(0);
            result.setFailedCount(0);
            result.setBatchId(generateBatchId(assessmentId));
            return result;
        }

        // 5. 幂等检查：今天已提醒过的用户跳过
        List<Long> todayReminded = attemptMapper.selectTodayRemindedUserIds(assessmentId, targetUserIds);
        Set<Long> todayRemindedSet = new HashSet<>(todayReminded);

        // 6. 重新校验每个用户仍然是未参加 & 有效学员
        Set<Long> skippedUserIds = new HashSet<>(todayReminded);
        List<Long> validUserIds = new ArrayList<>();
        for (Long uid : targetUserIds) {
            if (todayRemindedSet.contains(uid)) {
                continue; // 今天已提醒，跳过
            }
            validUserIds.add(uid);
        }

        // 如果非 remindAll，需要校验用户有效性
        if (!Boolean.TRUE.equals(request.getRemindAll())) {
            List<Long> confirmedNonParticipants = attemptMapper.selectNonParticipantUserIds(
                    assessmentId, assessment.getCourseId(), assessment.getCategoryId());
            Set<Long> confirmedSet = new HashSet<>(confirmedNonParticipants);
            Iterator<Long> it = validUserIds.iterator();
            while (it.hasNext()) {
                Long uid = it.next();
                if (!confirmedSet.contains(uid)) {
                    it.remove();
                    skippedUserIds.add(uid);
                }
            }
        }

        // 7. 生成批次号
        String batchId = generateBatchId(assessmentId);

        // 8. 拼接消息内容
        String messageContent = buildReminderContent(assessment, course, request.getContent());

        // 9. 批量创建消息
        int sentCount = 0;
        int failedCount = 0;
        List<CourseStudentMessage> messagesToSend = new ArrayList<>();

        for (Long uid : validUserIds) {
            try {
                CourseStudentMessage msg = new CourseStudentMessage();
                msg.setReceiverId(uid);
                msg.setSenderId(adminId);
                msg.setCourseId(assessment.getCourseId());
                msg.setCourseTitle(course.getTitle());
                msg.setAssessmentId(assessmentId);
                msg.setBatchId(batchId);
                msg.setMessageType("ASSESSMENT_REMINDER");
                msg.setContent(messageContent);
                msg.setCreatedAt(LocalDateTime.now());
                messageMapper.insert(msg);
                messagesToSend.add(msg);
                sentCount++;
            } catch (Exception e) {
                log.error("创建提醒消息失败: assessmentId={}, userId={}", assessmentId, uid, e);
                failedCount++;
            }
        }

        int skippedCount = requestedCount - sentCount - failedCount;

        // 10. 更新频率限制
        rateLimitMap.put(rateLimitKey, LocalDateTime.now());

        // 11. 事务提交后推送 WebSocket
        final int finalSentCount = sentCount;
        final int finalFailedCount = failedCount;
        final int finalRequestedCount = requestedCount;
        final Long finalAssessmentId = assessmentId;
        final String finalBatchId = batchId;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (CourseStudentMessage msg : messagesToSend) {
                    try {
                        pushWebSocket(msg);
                    } catch (Exception e) {
                        log.warn("推送提醒WebSocket异常: messageId={}, receiverId={}",
                                msg.getId(), msg.getReceiverId(), e);
                    }
                }
                log.info("考核提醒发送完成: assessmentId={}, batchId={}, sent={}, skipped={}, failed={}",
                        finalAssessmentId, finalBatchId, finalSentCount,
                        finalRequestedCount - finalSentCount - finalFailedCount, finalFailedCount);
            }
        });

        // 12. 返回结果
        ReminderSendResultVO result = new ReminderSendResultVO();
        result.setAssessmentId(assessmentId);
        result.setCourseId(assessment.getCourseId());
        result.setRequestedCount(requestedCount);
        result.setSentCount(sentCount);
        result.setSkippedCount(skippedCount);
        result.setFailedCount(failedCount);
        result.setBatchId(batchId);
        return result;
    }

    // ==================== 查询考核提醒发送历史 ====================

    @Override
    public PageResult<ReminderItemVO> listReminders(Long assessmentId, ReminderQueryDTO query) {
        getAssessmentOrThrow(assessmentId);

        // 校验 readStatus
        String readStatus = query.getReadStatus();
        if (readStatus != null && !readStatus.isBlank()) {
            readStatus = readStatus.trim().toUpperCase();
            if (!"ALL".equals(readStatus) && !"READ".equals(readStatus) && !"UNREAD".equals(readStatus)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "readStatus 参数不合法");
            }
            if ("ALL".equals(readStatus)) {
                readStatus = null;
            }
        }

        Page<ReminderItemVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<ReminderItemVO> result = attemptMapper.selectReminderPage(
                page,
                assessmentId,
                query.getKeyword(),
                readStatus,
                query.getBatchId(),
                query.getSenderId(),
                query.getSentFrom(),
                query.getSentTo()
        );

        return new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }

    // ==================== 私有辅助方法 ====================

    private Assessment getAssessmentOrThrow(Long assessmentId) {
        Assessment assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }
        return assessment;
    }

    private String generateBatchId(Long assessmentId) {
        return "AR-" + assessmentId + "-" + LocalDateTime.now().format(BATCH_DT_FMT);
    }

    private String buildReminderContent(Assessment assessment, Course course, String adminNote) {
        StringBuilder sb = new StringBuilder();
        sb.append("【考核提醒】\n\n");
        sb.append("课程：").append(course.getTitle()).append("\n");
        sb.append("考核：").append(assessment.getTitle()).append("\n");

        if (assessment.getStartAt() != null) {
            sb.append("开考时间：").append(assessment.getStartAt().format(DISPLAY_DT_FMT)).append("\n");
        }
        if (assessment.getEndAt() != null) {
            sb.append("最晚开考时间：").append(assessment.getEndAt().format(DISPLAY_DT_FMT)).append("\n");
        }

        sb.append("\n您尚未参加本场考核，请在规定时间内完成。");

        if (adminNote != null && !adminNote.trim().isEmpty()) {
            sb.append("\n管理员备注：").append(adminNote.trim());
        }

        return sb.toString();
    }

    private void pushWebSocket(CourseStudentMessage msg) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("messageId", msg.getId());
            data.put("courseId", msg.getCourseId());
            data.put("courseTitle", msg.getCourseTitle());
            data.put("assessmentId", msg.getAssessmentId());
            data.put("content", msg.getContent());
            data.put("read", false);
            data.put("createdAt", toOffset(msg.getCreatedAt()).toString());

            Map<String, Object> envelope = new HashMap<>();
            envelope.put("event", "COURSE_STUDENT_MESSAGE_CREATED");
            envelope.put("eventId", "message:" + msg.getId());
            envelope.put("occurredAt", toOffset(msg.getCreatedAt()).toString());
            envelope.put("data", data);

            webSocketHandler.sendToUser(msg.getReceiverId(), envelope);
        } catch (Exception e) {
            log.warn("推送WebSocket消息异常: messageId={}", msg.getId(), e);
        }
    }

    private OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt != null ? ldt.atOffset(ZoneOffset.ofHours(8)) : null;
    }
}
