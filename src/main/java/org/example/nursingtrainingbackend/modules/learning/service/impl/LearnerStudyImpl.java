package org.example.nursingtrainingbackend.modules.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.course.entity.*;
import org.example.nursingtrainingbackend.modules.course.mapper.*;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.Article;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.service.PptService;
import org.example.nursingtrainingbackend.modules.courseware.ppt.vo.PptPreviewFile;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.Video;
import org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoMapper;
import org.example.nursingtrainingbackend.modules.learning.dto.VideoProgressRequest;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCoursePointProgress;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCourseProgress;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCourseResourceProgress;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCoursePointProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCourseProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCourseResourceProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.service.LearnerStudy;
import org.example.nursingtrainingbackend.modules.learning.vo.CourseStudyVO;
import org.example.nursingtrainingbackend.modules.learning.vo.VideoProgressVO;
import org.example.nursingtrainingbackend.modules.user.entity.Department;
import org.example.nursingtrainingbackend.modules.user.mapper.DepartmentMapper;
import org.example.nursingtrainingbackend.modules.tag.entity.Tag;
import org.example.nursingtrainingbackend.modules.tag.mapper.TagMapper;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.nursingtrainingbackend.modules.learning.entity.UserLearningRecord;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserLearningRecordMapper;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearnerStudyImpl implements LearnerStudy {

    private static final String VIDEO_PROGRESS_KEY_PREFIX = "nursing:progress:video:";
    private static final String VIDEO_PROGRESS_DIRTY_SET = "nursing:progress:video:dirty";
    private static final long VIDEO_PROGRESS_TTL_HOURS = 24;

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CourseDepartmentMapper courseDepartmentMapper;
    private final CourseChapterMapper courseChapterMapper;
    private final CoursePointMapper coursePointMapper;
    private final CoursePointArticleMapper coursePointArticleMapper;
    private final CoursePointVideoMapper coursePointVideoMapper;
    private final CoursePointPptMapper coursePointPptMapper;
    private final ArticleMapper articleMapper;
    private final VideoMapper videoMapper;
    private final PptMapper pptMapper;
    private final PptService pptService;
    private final UserCourseProgressMapper userCourseProgressMapper;
    private final UserCoursePointProgressMapper userCoursePointProgressMapper;
    private final UserCourseResourceProgressMapper userCourseResourceProgressMapper;
    private final UserLearningRecordMapper userLearningRecordMapper;
    private final DepartmentMapper departmentMapper;
    private final TagMapper tagMapper;
    private final CourseTagMapper courseTagMapper;
    private final StringRedisTemplate redisTemplate;
    /** 获取课程学习页所需的课程点、资源和导航数据。 */

    @Override
    @Transactional
    public CourseStudyVO getCourseStudy(Long courseId, Long pointId, String activeType) {
        Long userId = SecurityUtils.currentUserId();
        User user = validateLearner(userId);
        LocalDateTime now = LocalDateTime.now();

        // 1. 验证课程可见性
        Course course = validateCourseVisible(courseId, user.getDeptId());

        // 2. 验证课程点有效性
        CoursePoint point = coursePointMapper.selectById(pointId);
        if (point == null || !point.getCourseId().equals(courseId)
                || point.getStatus() != 1 || point.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.LEARNER_POINT_NOT_FOUND);
        }

        // 3. 获取章节信息
        CourseChapter chapter = courseChapterMapper.selectById(point.getChapterId());

        // 4. 更新/创建课程点进度 → LEARNING
        UserCoursePointProgress pointProgress = getOrCreatePointProgress(userId, courseId, pointId, now);

        // 5. 写入进入学习页记录（action_type=1/2/6，5分钟防重复）
        // 必须在 updateCourseProgressToLearning 之前调用，以正确判断首次/继续/复习
        writeEnterStudyRecord(userId, courseId, pointId, now);

        // 5.1 更新课程进度为 LEARNING，写入 last_point_id
        updateCourseProgressToLearning(userId, courseId, pointId, now);

        // 6. 构建课程概要
        CourseStudyVO.CourseSummaryVO courseVO = buildCourseSummary(course, userId);

        // 7. 构建当前课程点
        String resolvedActiveType = resolveActiveType(pointId, activeType);
        CourseStudyVO.CurrentPointVO currentPointVO = buildCurrentPoint(point, chapter, pointProgress, resolvedActiveType);

        // 8. 构建三类课件 Tab
        CourseStudyVO.ResourceTabVO tabsVO = buildResourceTabs(pointId, userId);

        // 9. 构建导航
        CourseStudyVO.NavigationVO navigationVO = buildNavigation(courseId, pointId);

        // 10. 组装响应
        CourseStudyVO vo = new CourseStudyVO();
        vo.setCourse(courseVO);
        vo.setCurrentPoint(currentPointVO);
        vo.setTabs(tabsVO);
        vo.setNavigation(navigationVO);
        return vo;
    }
    /** 标记学习资源完成，并级联更新课程点和课程进度。 */

    @Override
    @Transactional
    public void completeResource(Long courseId, Long coursePointId, Integer resourceType, Long resourceId) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<UserCourseResourceProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseResourceProgress::getUserId, userId)
               .eq(UserCourseResourceProgress::getCourseId, courseId)
               .eq(UserCourseResourceProgress::getCoursePointId, coursePointId)
               .eq(UserCourseResourceProgress::getResourceType, resourceType)
               .eq(UserCourseResourceProgress::getResourceId, resourceId);
        UserCourseResourceProgress progress = userCourseResourceProgressMapper.selectOne(wrapper);

        if (progress == null) {
            progress = new UserCourseResourceProgress();
            progress.setUserId(userId);
            progress.setCourseId(courseId);
            progress.setCoursePointId(coursePointId);
            progress.setResourceType(resourceType);
            progress.setResourceId(resourceId);
            progress.setStartedAt(now);
            progress.setCreatedAt(now);
        }

        progress.setStatus(2);
        progress.setCompletedAt(now);
        progress.setProgressPercent(BigDecimal.valueOf(100));
        progress.setUpdatedAt(now);

        if (progress.getId() == null) {
            userCourseResourceProgressMapper.insert(progress);
        } else {
            userCourseResourceProgressMapper.updateById(progress);
        }

        try {
            writeResourceCompleteRecordIfNeeded(userId, courseId, coursePointId, resourceType, resourceId, now);
        } catch (Exception e) {
            log.error("写入资源完成记录失败, userId={}, resourceId={}", userId, resourceId, e);
        }

        recalculatePointProgress(userId, courseId, coursePointId, now);
        recalculateCourseProgress(userId, courseId, now);
    }
    /** 校验学员访问权限并返回PPT预览文件流。 */

    @Override
    public PptPreviewFile getPptPreview(Long coursePointId, Long pptId) {
        Long userId = SecurityUtils.currentUserId();
        User user = validateLearner(userId);
        CoursePoint point = coursePointMapper.selectById(coursePointId);
        if (point == null || !Integer.valueOf(1).equals(point.getStatus()) || point.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.LEARNER_POINT_NOT_FOUND);
        }
        validateCourseVisible(point.getCourseId(), user.getDeptId());

        Long relationCount = coursePointPptMapper.selectCount(
                new LambdaQueryWrapper<CoursePointPpt>()
                        .eq(CoursePointPpt::getCoursePointId, coursePointId)
                        .eq(CoursePointPpt::getPptId, pptId));
        if (relationCount == null || relationCount == 0) {
            throw new BusinessException(ErrorCode.LEARNER_POINT_NOT_FOUND);
        }
        return pptService.getPreviewFile(pptId);
    }
    /** 上报视频播放进度，并在满足条件时完成资源。 */

    @Override
    public VideoProgressVO reportVideoProgress(Long courseId, Long coursePointId, Long videoId, VideoProgressRequest request) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);

        if (!request.isEventValid()) {
            throw new BusinessException(ErrorCode.LEARNER_VIDEO_PROGRESS_INVALID);
        }

        String cacheKey = VIDEO_PROGRESS_KEY_PREFIX + userId + ":" + coursePointId + ":" + videoId;
        LocalDateTime now = LocalDateTime.now();

        // 取历史最远播放位置
        int reportedCurrentSeconds = request.isVideoEnded() ? request.durationSeconds() : request.currentSeconds();
        int currentMax = reportedCurrentSeconds;
        Object existingMaxObj = redisTemplate.opsForHash().get(cacheKey, "maxPositionSeconds");
        if (existingMaxObj != null) {
            try {
                currentMax = Math.max(currentMax, Integer.parseInt(existingMaxObj.toString()));
            } catch (NumberFormatException ignored) {}
        }
        if (request.isVideoEnded()) {
            currentMax = request.durationSeconds();
        }

        // 写入 Redis Hash
        Map<String, String> progressData = new HashMap<>();
        progressData.put("userId", userId.toString());
        progressData.put("courseId", courseId.toString());
        progressData.put("coursePointId", coursePointId.toString());
        progressData.put("videoId", videoId.toString());
        progressData.put("currentSeconds", String.valueOf(reportedCurrentSeconds));
        progressData.put("maxPositionSeconds", String.valueOf(currentMax));
        progressData.put("durationSeconds", request.durationSeconds().toString());
        progressData.put("eventType", request.eventType() != null ? request.eventType() : "AUTO");
        progressData.put("lastReportedAt", now.toString());
        progressData.put("ended", request.ended().toString());

        redisTemplate.opsForHash().putAll(cacheKey, progressData);
        redisTemplate.expire(cacheKey, VIDEO_PROGRESS_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForSet().add(VIDEO_PROGRESS_DIRTY_SET, cacheKey);

        // 终止事件（暂停/退出/完成）→ 即时同步 MySQL
        boolean isTerminalEvent = request.isVideoEnded()
                || "PAUSE".equals(request.eventType())
                || "LEAVE".equals(request.eventType());

        if (isTerminalEvent) {
            try {
                syncSingleProgressToMysql(cacheKey);
            } catch (Exception e) {
                log.error("即时同步视频进度失败, userId={}, videoId={}", userId, videoId, e);
            }
        }

        boolean completed = request.isVideoEnded()
                || (request.durationSeconds() > 0 && currentMax >= request.durationSeconds() - 2);
        BigDecimal progressPercent = BigDecimal.valueOf(Math.min(currentMax, request.durationSeconds()))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(request.durationSeconds()), 2, RoundingMode.HALF_UP);

        return VideoProgressVO.builder()
                .videoId(videoId)
                .learningStatus(completed ? "COMPLETED" : "LEARNING")
                .progressPercent(completed ? BigDecimal.valueOf(100) : progressPercent)
                .lastPositionSeconds(reportedCurrentSeconds)
                .maxPositionSeconds(Math.min(currentMax, request.durationSeconds()))
                .completed(completed)
                .pointCompleted(false)
                .courseCompleted(false)
                .build();
    }

    /**
     * 定时批量同步：每 30 秒扫描脏数据，同步到 MySQL
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void batchSyncVideoProgress() {
        Set<String> dirtyKeys;
        try {
            dirtyKeys = redisTemplate.opsForSet().members(VIDEO_PROGRESS_DIRTY_SET);
        } catch (Exception e) {
            log.warn("读取视频进度脏数据集合失败", e);
            return;
        }
        if (dirtyKeys == null || dirtyKeys.isEmpty()) {
            return;
        }

        for (String key : dirtyKeys) {
            try {
                syncSingleProgressToMysql(key);
            } catch (Exception e) {
                log.error("同步视频进度失败, key={}", key, e);
            }
        }
    }

    /**
     * 单条同步：Redis → MySQL，并级联重算课程点/课程进度
     */
    private void syncSingleProgressToMysql(String cacheKey) {
        Map<Object, Object> data = redisTemplate.opsForHash().entries(cacheKey);
        if (data.isEmpty()) {
            redisTemplate.opsForSet().remove(VIDEO_PROGRESS_DIRTY_SET, cacheKey);
            return;
        }

        Long userId = Long.parseLong(data.get("userId").toString());
        Long courseId = Long.parseLong(data.get("courseId").toString());
        Long coursePointId = Long.parseLong(data.get("coursePointId").toString());
        Long videoId = Long.parseLong(data.get("videoId").toString());
        Integer currentSeconds = Integer.parseInt(data.get("currentSeconds").toString());
        Integer maxPositionSeconds = Integer.parseInt(data.get("maxPositionSeconds").toString());
        Integer durationSeconds = Integer.parseInt(data.get("durationSeconds").toString());
        boolean ended = Boolean.parseBoolean(data.getOrDefault("ended", "false").toString());
        LocalDateTime now = LocalDateTime.now();

        // 查找或创建资源进度记录
        LambdaQueryWrapper<UserCourseResourceProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseResourceProgress::getUserId, userId)
               .eq(UserCourseResourceProgress::getCourseId, courseId)
               .eq(UserCourseResourceProgress::getCoursePointId, coursePointId)
               .eq(UserCourseResourceProgress::getResourceType, 2)
               .eq(UserCourseResourceProgress::getResourceId, videoId);
        UserCourseResourceProgress progress = userCourseResourceProgressMapper.selectOne(wrapper);

        if (progress == null) {
            progress = new UserCourseResourceProgress();
            progress.setUserId(userId);
            progress.setCourseId(courseId);
            progress.setCoursePointId(coursePointId);
            progress.setResourceType(2);
            progress.setResourceId(videoId);
            progress.setDurationSeconds(durationSeconds);
            progress.setStartedAt(now);
            progress.setCreatedAt(now);
        }

        progress.setLastPositionSeconds(currentSeconds);
        progress.setMaxPositionSeconds(Math.max(maxPositionSeconds,
                progress.getMaxPositionSeconds() != null ? progress.getMaxPositionSeconds() : 0));
        progress.setUpdatedAt(now);

        // 计算百分比
        if (durationSeconds > 0) {
            BigDecimal percent = BigDecimal.valueOf(Math.min(maxPositionSeconds, durationSeconds))
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(durationSeconds), 2, RoundingMode.HALF_UP);
            progress.setProgressPercent(percent);
        }

        // 判断完成：ended=true 或 最远位置 >= 总时长-2秒
        if (ended || (durationSeconds > 0 && maxPositionSeconds >= durationSeconds - 2)) {
            progress.setStatus(2);
            progress.setCompletedAt(now);
            progress.setProgressPercent(BigDecimal.valueOf(100));
        } else {
            progress.setStatus(1);
        }

        if (progress.getId() == null) {
            userCourseResourceProgressMapper.insert(progress);
        } else {
            userCourseResourceProgressMapper.updateById(progress);
        }

        // 视频完成时写入 COMPLETE_RESOURCE 记录（action_type=3）
        if (progress.getStatus() == 2) {
            try {
                writeResourceCompleteRecordIfNeeded(userId, courseId, coursePointId, 2, videoId, now);
            } catch (Exception e) {
                log.error("写入视频完成记录失败, userId={}, videoId={}", userId, videoId, e);
            }
        }

        // 级联重算课程点进度 → 课程进度
        recalculatePointProgress(userId, courseId, coursePointId, now);
        recalculateCourseProgress(userId, courseId, now);

        // 清除 Redis 缓存
        redisTemplate.delete(cacheKey);
        redisTemplate.opsForSet().remove(VIDEO_PROGRESS_DIRTY_SET, cacheKey);
    }

    /**
     * 级联重算课程点进度
     */
    private void recalculatePointProgress(Long userId, Long courseId, Long pointId, LocalDateTime now) {
        List<UserCourseResourceProgress> pointResources = userCourseResourceProgressMapper.selectList(
                new LambdaQueryWrapper<UserCourseResourceProgress>()
                        .eq(UserCourseResourceProgress::getUserId, userId)
                        .eq(UserCourseResourceProgress::getCoursePointId, pointId));

        boolean allCompleted = !pointResources.isEmpty() &&
                pointResources.stream().allMatch(r -> r.getStatus() == 2);

        UserCoursePointProgress pointProgress = userCoursePointProgressMapper.selectOne(
                new LambdaQueryWrapper<UserCoursePointProgress>()
                        .eq(UserCoursePointProgress::getUserId, userId)
                        .eq(UserCoursePointProgress::getCoursePointId, pointId));

        if (pointProgress == null) {
            pointProgress = new UserCoursePointProgress();
            pointProgress.setUserId(userId);
            pointProgress.setCourseId(courseId);
            pointProgress.setCoursePointId(pointId);
            pointProgress.setStartedAt(now);
            pointProgress.setCreatedAt(now);
        }

        boolean wasCompleted = pointProgress.getStatus() != null && pointProgress.getStatus() == 2;

        if (allCompleted) {
            pointProgress.setStatus(2);
            pointProgress.setCompletedAt(now);
        } else {
            pointProgress.setStatus(1);
            if (pointProgress.getCompletedAt() != null) pointProgress.setCompletedAt(null);
        }
        pointProgress.setUpdatedAt(now);

        if (pointProgress.getId() == null) {
            userCoursePointProgressMapper.insert(pointProgress);
        } else {
            userCoursePointProgressMapper.updateById(pointProgress);
        }

        if (allCompleted && !wasCompleted) {
            try {
                CoursePoint point = coursePointMapper.selectById(pointId);
                String title = "完成了课程点《" + (point != null ? point.getTitle() : "") + "》";
                insertLearningRecord(userId, courseId, pointId, 4, null, null, title, now);
            } catch (Exception e) {
                log.error("写入课程点完成记录失败, userId={}, pointId={}", userId, pointId, e);
            }
        }
    }

    /**
     * 级联重算课程进度
     */
    private void recalculateCourseProgress(Long userId, Long courseId, LocalDateTime now) {
        List<UserCoursePointProgress> allPointProgress = userCoursePointProgressMapper.selectList(
                new LambdaQueryWrapper<UserCoursePointProgress>()
                        .eq(UserCoursePointProgress::getUserId, userId)
                        .eq(UserCoursePointProgress::getCourseId, courseId));

        long totalPoints = coursePointMapper.selectCount(
                new LambdaQueryWrapper<CoursePoint>()
                        .eq(CoursePoint::getCourseId, courseId)
                        .eq(CoursePoint::getStatus, 1)
                        .isNull(CoursePoint::getDeletedAt));

        long completedPoints = allPointProgress.stream().filter(p -> p.getStatus() == 2).count();
        long startedPoints = allPointProgress.stream().filter(p -> p.getStatus() >= 1).count();

        UserCourseProgress courseProgress = userCourseProgressMapper.selectOne(
                new LambdaQueryWrapper<UserCourseProgress>()
                        .eq(UserCourseProgress::getUserId, userId)
                        .eq(UserCourseProgress::getCourseId, courseId));

        if (courseProgress == null) {
            if (startedPoints > 0) {
                courseProgress = new UserCourseProgress();
                courseProgress.setUserId(userId);
                courseProgress.setCourseId(courseId);
                courseProgress.setStartedAt(now);
                courseProgress.setCreatedAt(now);
            } else {
                return;
            }
        }

        boolean wasCompleted = courseProgress.getStatus() != null && courseProgress.getStatus() == 2;

        if (totalPoints > 0 && completedPoints == totalPoints) {
            courseProgress.setStatus(2);
            courseProgress.setCompletedAt(now);
            courseProgress.setProgressPercent(BigDecimal.valueOf(100));
        } else if (startedPoints > 0) {
            courseProgress.setStatus(1);
            if (totalPoints > 0) {
                BigDecimal percent = BigDecimal.valueOf(completedPoints)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalPoints), 2, RoundingMode.HALF_UP);
                courseProgress.setProgressPercent(percent);
            }
            if (courseProgress.getCompletedAt() != null) courseProgress.setCompletedAt(null);
        } else {
            courseProgress.setStatus(0);
            courseProgress.setProgressPercent(BigDecimal.ZERO);
            courseProgress.setStartedAt(null);
            courseProgress.setCompletedAt(null);
        }

        courseProgress.setUpdatedAt(now);
        if (courseProgress.getId() == null) {
            userCourseProgressMapper.insert(courseProgress);
        } else {
            userCourseProgressMapper.updateById(courseProgress);
        }

        if (totalPoints > 0 && completedPoints == totalPoints && !wasCompleted) {
            try {
                Course course = courseMapper.selectById(courseId);
                String title = "完成了课程《" + (course != null ? course.getTitle() : "") + "》";
                insertLearningRecord(userId, courseId, null, 5, null, null, title, now);
            } catch (Exception e) {
                log.error("写入课程完成记录失败, userId={}, courseId={}", userId, courseId, e);
            }
        }
    }

    /**
     * 写入进入课程学习页记录
     * - 首次进入：action_type=1
     * - 已开始未完成再次进入：action_type=2，5分钟防重复
     * - 已完成再次进入：action_type=6，5分钟防重复
     */
    private void writeEnterStudyRecord(Long userId, Long courseId, Long pointId, LocalDateTime now) {
        try {
            LambdaQueryWrapper<UserCourseProgress> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserCourseProgress::getUserId, userId)
                    .eq(UserCourseProgress::getCourseId, courseId);
            UserCourseProgress courseProgress = userCourseProgressMapper.selectOne(wrapper);

            int actionType;
            if (courseProgress != null && courseProgress.getStatus() != null && courseProgress.getStatus() == 2) {
                actionType = 6;
            } else if (courseProgress != null && courseProgress.getStatus() != null && courseProgress.getStatus() >= 1) {
                actionType = 2;
            } else {
                actionType = 1;
            }

            // 5分钟内同一课程只写入一条进入记录（不论 actionType 是 1/2/6）
            // 防止首次进入写入 action_type=1 后，再次进入因 action_type=2 不同而绕过防重
            if (hasRecentEnterRecord(userId, courseId, now)) {
                return;
            }

            String title = buildEnterTitle(courseProgress, actionType);
            insertLearningRecord(userId, courseId, pointId, actionType, null, null, title, now);
        } catch (Exception e) {
            log.error("写入进入学习页记录失败, userId={}, courseId={}", userId, courseId, e);
        }
    }

    private String buildEnterTitle(UserCourseProgress courseProgress, int actionType) {
        String actionName = switch (actionType) {
            case 1 -> "开始学习";
            case 2 -> "继续学习";
            case 6 -> "复习";
            default -> "进入学习";
        };
        if (courseProgress != null) {
            Course course = courseMapper.selectById(courseProgress.getCourseId());
            String courseTitle = course != null ? course.getTitle() : "未知课程";
            return actionName + "《" + courseTitle + "》";
        }
        return actionName;
    }

    /**
     * 5分钟内防重复：检查是否已有相同 action_type 的记录
     */
    private boolean hasRecentRecord(Long userId, Long courseId, int actionType, LocalDateTime now) {
        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId)
                .eq(UserLearningRecord::getCourseId, courseId)
                .eq(UserLearningRecord::getActionType, actionType)
                .ge(UserLearningRecord::getCreatedAt, now.minusMinutes(5));
        Long count = userLearningRecordMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    /**
     * 5分钟内防重复：检查是否已有任意进入类型（1/2/6）的记录
     * 防止首次进入写入 action_type=1 后，再次进入因 action_type=2 不同而绕过防重
     */
    private boolean hasRecentEnterRecord(Long userId, Long courseId, LocalDateTime now) {
        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId)
                .eq(UserLearningRecord::getCourseId, courseId)
                .in(UserLearningRecord::getActionType, 1, 2, 6)
                .ge(UserLearningRecord::getCreatedAt, now.minusMinutes(5));
        Long count = userLearningRecordMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private void insertLearningRecord(Long userId, Long courseId, Long coursePointId,
                                      int actionType, Integer resourceType, Long resourceId,
                                      String title, LocalDateTime time) {
        UserLearningRecord record = new UserLearningRecord();
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setCoursePointId(coursePointId);
        record.setActionType(actionType);
        record.setResourceType(resourceType);
        record.setResourceId(resourceId);
        record.setTitle(title);
        record.setCreatedAt(time);
        userLearningRecordMapper.insert(record);
    }

    /**
     * 写入课件完成记录（action_type=3），防重复
     */
    private void writeResourceCompleteRecordIfNeeded(Long userId, Long courseId, Long coursePointId,
                                                      Integer resourceType, Long resourceId, LocalDateTime now) {
        // 检查是否已有完成记录
        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId)
                .eq(UserLearningRecord::getCourseId, courseId)
                .eq(UserLearningRecord::getActionType, 3)
                .eq(UserLearningRecord::getResourceType, resourceType)
                .eq(UserLearningRecord::getResourceId, resourceId);
        Long count = userLearningRecordMapper.selectCount(wrapper);
        if (count != null && count > 0) return;

        String typeName = switch (resourceType != null ? resourceType : 0) {
            case 1 -> "文章";
            case 2 -> "视频";
            case 3 -> "PPT";
            default -> "课件";
        };
        Course course = courseMapper.selectById(courseId);
        String courseTitle = course != null ? course.getTitle() : "未知课程";
        String title = "完成了《" + courseTitle + "》的" + typeName;
        insertLearningRecord(userId, courseId, coursePointId, 3, resourceType, resourceId, title, now);
    }

    // ==================== 私有辅助方法 ====================

    private User validateLearner(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.LEARNER_NOT_FOUND);
        }
        if (user.getRoleType() != 1 || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (user.getDeptId() == null) {
            throw new BusinessException(ErrorCode.LEARNER_DEPT_NOT_BINDIED);
        }
        return user;
    }

    private Course validateCourseVisible(Long courseId, Long deptId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.LEARNER_COURSE_NOT_VISIBLE);
        }
        if (course.getStatus() != 1) {
            throw new BusinessException(ErrorCode.LEARNER_COURSE_NOT_PUBLISHED);
        }
        LambdaQueryWrapper<CourseDepartment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseDepartment::getCourseId, courseId)
               .eq(CourseDepartment::getDepartmentId, deptId);
        Long count = courseDepartmentMapper.selectCount(wrapper);
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.LEARNER_COURSE_NOT_VISIBLE);
        }
        return course;
    }

    /**
     * 创建或更新课程点进度为 LEARNING
     */
    private UserCoursePointProgress getOrCreatePointProgress(Long userId, Long courseId, Long pointId, LocalDateTime now) {
        LambdaQueryWrapper<UserCoursePointProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoursePointProgress::getUserId, userId)
               .eq(UserCoursePointProgress::getCoursePointId, pointId);
        UserCoursePointProgress progress = userCoursePointProgressMapper.selectOne(wrapper);

        if (progress == null) {
            progress = new UserCoursePointProgress();
            progress.setUserId(userId);
            progress.setCourseId(courseId);
            progress.setCoursePointId(pointId);
            progress.setStatus(1); // LEARNING
            progress.setStartedAt(now);
            progress.setCreatedAt(now);
            progress.setUpdatedAt(now);
            userCoursePointProgressMapper.insert(progress);
        } else if (progress.getStatus() != 2) {
            // 非已完成状态 → 更新为学习中
            progress.setStatus(1);
            progress.setUpdatedAt(now);
            if (progress.getStartedAt() == null) {
                progress.setStartedAt(now);
            }
            userCoursePointProgressMapper.updateById(progress);
        }
        return progress;
    }

    /**
     * 更新课程进度为学习中，写入 last_point_id
     */
    private void updateCourseProgressToLearning(Long userId, Long courseId, Long pointId, LocalDateTime now) {
        LambdaQueryWrapper<UserCourseProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseProgress::getUserId, userId)
               .eq(UserCourseProgress::getCourseId, courseId);
        UserCourseProgress progress = userCourseProgressMapper.selectOne(wrapper);

        if (progress == null) {
            progress = new UserCourseProgress();
            progress.setUserId(userId);
            progress.setCourseId(courseId);
            progress.setStatus(1);
            progress.setProgressPercent(BigDecimal.ZERO);
            progress.setLastPointId(pointId);
            progress.setStartedAt(now);
            progress.setCreatedAt(now);
            progress.setUpdatedAt(now);
            userCourseProgressMapper.insert(progress);
        } else {
            if (!Integer.valueOf(2).equals(progress.getStatus())) {
                progress.setStatus(1);
            }
            progress.setLastPointId(pointId);
            progress.setUpdatedAt(now);
            userCourseProgressMapper.updateById(progress);
        }
    }

    /**
     * 构建课程概要
     */
    private CourseStudyVO.CourseSummaryVO buildCourseSummary(Course course, Long userId) {
        CourseStudyVO.CourseSummaryVO vo = new CourseStudyVO.CourseSummaryVO();
        vo.setCourseId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setCoverUrl(course.getCoverUrl());

        // 讲师信息
        if (course.getInstructorId() != null) {
            User instructor = userMapper.selectById(course.getInstructorId());
            if (instructor != null) {
                vo.setInstructorName(instructor.getRealName());
                if (instructor.getDeptId() != null) {
                    Department dept = departmentMapper.selectById(instructor.getDeptId());
                    vo.setInstructorDepartment(dept != null ? dept.getName() : null);
                }
            }
        }

        // 课程标签
        List<CourseTag> courseTags = courseTagMapper.selectList(
                new LambdaQueryWrapper<CourseTag>().eq(CourseTag::getCourseId, course.getId()));
        if (!courseTags.isEmpty()) {
            List<Long> tagIds = courseTags.stream().map(CourseTag::getTagId).toList();
            List<Tag> tags = tagMapper.selectBatchIds(tagIds);
            vo.setTags(tags.stream().map(Tag::getName).toList());
        }

        // 课程点总数（启用、未删除）
        Long pointCount = coursePointMapper.selectCount(
                new LambdaQueryWrapper<CoursePoint>()
                        .eq(CoursePoint::getCourseId, course.getId())
                        .eq(CoursePoint::getStatus, 1));
        vo.setPointCount(pointCount.intValue());

        // 已完成课程点数
        Long completedCount = userCoursePointProgressMapper.selectCount(
                new LambdaQueryWrapper<UserCoursePointProgress>()
                        .eq(UserCoursePointProgress::getUserId, userId)
                        .eq(UserCoursePointProgress::getCourseId, course.getId())
                        .eq(UserCoursePointProgress::getStatus, 2));
        vo.setCompletedPointCount(completedCount.intValue());

        // 课程进度
        LambdaQueryWrapper<UserCourseProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseProgress::getUserId, userId)
               .eq(UserCourseProgress::getCourseId, course.getId());
        UserCourseProgress progress = userCourseProgressMapper.selectOne(wrapper);
        vo.setProgressPercent(progress != null && progress.getProgressPercent() != null
                ? progress.getProgressPercent() : BigDecimal.ZERO);

        return vo;
    }

    /**
     * 构建当前课程点 VO
     */
    private CourseStudyVO.CurrentPointVO buildCurrentPoint(CoursePoint point, CourseChapter chapter,
                                              UserCoursePointProgress progress, String activeType) {
        CourseStudyVO.CurrentPointVO vo = new CourseStudyVO.CurrentPointVO();
        vo.setPointId(point.getId());
        vo.setChapterId(point.getChapterId());
        vo.setChapterTitle(chapter != null ? chapter.getTitle() : null);
        vo.setTitle(point.getTitle());
        vo.setDescription(point.getDescription());
        vo.setRequired(point.getRequired() != null && point.getRequired() == 1);
        vo.setActiveType(activeType);

        if (progress == null || progress.getStatus() == 0) {
            vo.setLearningStatus("NOT_STARTED");
        } else if (progress.getStatus() == 1) {
            vo.setLearningStatus("LEARNING");
        } else {
            vo.setLearningStatus("COMPLETED");
        }
        return vo;
    }

    /**
     * 解析 activeType：未传时按 VIDEO > ARTICLE > PPT 选择第一个有课件的 Tab
     */
    private String resolveActiveType(Long pointId, String activeType) {
        if (activeType != null && !activeType.isBlank()) {
            return activeType.toUpperCase();
        }
        long videoCount = coursePointVideoMapper.selectCount(
                new LambdaQueryWrapper<CoursePointVideo>().eq(CoursePointVideo::getCoursePointId, pointId));
        if (videoCount > 0) return "VIDEO";

        long articleCount = coursePointArticleMapper.selectCount(
                new LambdaQueryWrapper<CoursePointArticle>().eq(CoursePointArticle::getCoursePointId, pointId));
        if (articleCount > 0) return "ARTICLE";

        long pptCount = coursePointPptMapper.selectCount(
                new LambdaQueryWrapper<CoursePointPpt>().eq(CoursePointPpt::getCoursePointId, pointId));
        if (pptCount > 0) return "PPT";

        return "VIDEO";
    }

    /**
     * 构建三类课件 Tab
     */
    private CourseStudyVO.ResourceTabVO buildResourceTabs(Long pointId, Long userId) {
        // 加载课件进度 key="TYPE:resourceId:coursePointId"
        Map<String, UserCourseResourceProgress> rpMap = loadResourceProgressMap(pointId, userId);

        // 1. 视频
        List<CoursePointVideo> cpvs = coursePointVideoMapper.selectList(
                new LambdaQueryWrapper<CoursePointVideo>()
                        .eq(CoursePointVideo::getCoursePointId, pointId)
                        .orderByAsc(CoursePointVideo::getSort));
        List<CourseStudyVO.VideoVO> videos = cpvs.stream().map(cpv -> {
            Video video = videoMapper.selectById(cpv.getVideoId());
            if (video == null || video.getStatus() != 1 || video.getDeletedAt() != null) return null;

            String key = "VIDEO:" + video.getId() + ":" + pointId;
            UserCourseResourceProgress rp = rpMap.get(key);

            CourseStudyVO.VideoVO vo = new CourseStudyVO.VideoVO();
            vo.setVideoId(video.getId());
            vo.setTitle(video.getTitle());
            vo.setDescription(video.getDescription());
            vo.setCoverUrl(video.getCoverUrl());
            vo.setPlayUrl(video.getVideoUrl());
            vo.setDurationSeconds(video.getDuration());
            vo.setAllowDrag(video.getAllowDrag() != null && video.getAllowDrag() == 1);
            vo.setAllowSpeed(video.getAllowSpeed() != null && video.getAllowSpeed() == 1);

            if (rp == null) {
                vo.setLearningStatus("NOT_STARTED");
                vo.setProgressPercent(BigDecimal.ZERO);
                vo.setCompleted(false);
            } else {
                vo.setLearningStatus(rp.getStatus() == 2 ? "COMPLETED" : rp.getStatus() == 1 ? "LEARNING" : "NOT_STARTED");
                vo.setProgressPercent(rp.getProgressPercent() != null ? rp.getProgressPercent() : BigDecimal.ZERO);
                vo.setLastPositionSeconds(rp.getLastPositionSeconds());
                vo.setMaxPositionSeconds(rp.getMaxPositionSeconds());
                vo.setCompleted(rp.getStatus() == 2);
            }
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        // 2. 文章
        List<CoursePointArticle> cpas = coursePointArticleMapper.selectList(
                new LambdaQueryWrapper<CoursePointArticle>()
                        .eq(CoursePointArticle::getCoursePointId, pointId)
                        .orderByAsc(CoursePointArticle::getSort));
        List<CourseStudyVO.ArticleVO> articles = cpas.stream().map(cpa -> {
            Article article = articleMapper.selectById(cpa.getArticleId());
            if (article == null || article.getStatus() != 1 || article.getDeletedAt() != null) return null;

            String key = "ARTICLE:" + article.getId() + ":" + pointId;
            UserCourseResourceProgress rp = rpMap.get(key);

            CourseStudyVO.ArticleVO vo = new CourseStudyVO.ArticleVO();
            vo.setArticleId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setHtmlContent(article.getContent());
            vo.setAttachmentName(article.getAttachmentName());
            vo.setAttachmentPreviewUrl(article.getAttachmentUrl());
            vo.setAllowDownload(article.getAllowDownload() != null && article.getAllowDownload() == 1);

            if (rp == null) {
                vo.setLearningStatus("NOT_STARTED");
                vo.setProgressPercent(BigDecimal.ZERO);
                vo.setCompleted(false);
            } else {
                vo.setLearningStatus(rp.getStatus() == 2 ? "COMPLETED" : rp.getStatus() == 1 ? "LEARNING" : "NOT_STARTED");
                vo.setProgressPercent(rp.getProgressPercent() != null ? rp.getProgressPercent() : BigDecimal.ZERO);
                vo.setCompleted(rp.getStatus() == 2);
            }
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        // 3. PPT
        List<CoursePointPpt> cpps = coursePointPptMapper.selectList(
                new LambdaQueryWrapper<CoursePointPpt>()
                        .eq(CoursePointPpt::getCoursePointId, pointId)
                        .orderByAsc(CoursePointPpt::getSort));
        List<CourseStudyVO.PptVO> ppts = cpps.stream().map(cpp -> {
            Ppt ppt = pptMapper.selectById(cpp.getPptId());
            if (ppt == null || ppt.getStatus() != 1 || ppt.getDeletedAt() != null) return null;

            String key = "PPT:" + ppt.getId() + ":" + pointId;
            UserCourseResourceProgress rp = rpMap.get(key);

            CourseStudyVO.PptVO vo = new CourseStudyVO.PptVO();
            vo.setPptId(ppt.getId());
            vo.setTitle(ppt.getTitle());
            vo.setDescription(ppt.getDescription());
            vo.setCoverUrl(ppt.getCoverUrl());
            vo.setPreviewUrl(resolvePptPreviewUrl(pointId, ppt));
            vo.setPageCount(ppt.getPageCount());
            vo.setAllowDownload(ppt.getAllowDownload() != null && ppt.getAllowDownload() == 1);

            if (rp == null) {
                vo.setLearningStatus("NOT_STARTED");
                vo.setProgressPercent(BigDecimal.ZERO);
                vo.setCompleted(false);
            } else {
                vo.setLearningStatus(rp.getStatus() == 2 ? "COMPLETED" : rp.getStatus() == 1 ? "LEARNING" : "NOT_STARTED");
                vo.setProgressPercent(rp.getProgressPercent() != null ? rp.getProgressPercent() : BigDecimal.ZERO);
                vo.setCompleted(rp.getStatus() == 2);
            }
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        CourseStudyVO.ResourceTabVO tabs = new CourseStudyVO.ResourceTabVO();
        tabs.setVideos(videos);
        tabs.setArticles(articles);
        tabs.setPpts(ppts);
        return tabs;
    }

    /**
     * 查询课件进度，返回 key="TYPE:resourceId:coursePointId"
     */
    private Map<String, UserCourseResourceProgress> loadResourceProgressMap(Long pointId, Long userId) {
        LambdaQueryWrapper<UserCourseResourceProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseResourceProgress::getUserId, userId)
               .eq(UserCourseResourceProgress::getCoursePointId, pointId);
        List<UserCourseResourceProgress> progresses = userCourseResourceProgressMapper.selectList(wrapper);

        Map<String, UserCourseResourceProgress> map = new HashMap<>();
        for (UserCourseResourceProgress rp : progresses) {
            String typeName = switch (rp.getResourceType() != null ? rp.getResourceType() : 0) {
                case 1 -> "ARTICLE";
                case 2 -> "VIDEO";
                case 3 -> "PPT";
                default -> "UNKNOWN";
            };
            String key = typeName + ":" + rp.getResourceId() + ":" + rp.getCoursePointId();
            map.put(key, rp);
        }
        return map;
    }

    private String resolvePptPreviewUrl(Long pointId, Ppt ppt) {
        return "/api/learner/courses/points/" + pointId + "/ppts/" + ppt.getId() + "/preview";
    }

    /**
     * 构建上一课/下一课导航
     * 按 course_chapter.sort ASC, course_chapter.id ASC + course_point.sort ASC, course_point.id ASC
     */
    private CourseStudyVO.NavigationVO buildNavigation(Long courseId, Long pointId) {
        CourseStudyVO.NavigationVO nav = new CourseStudyVO.NavigationVO();

        // 获取课程所有启用章节，按 sort ASC, id ASC
        List<CourseChapter> chapters = courseChapterMapper.selectList(
                new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourseId, courseId)
                        .isNull(CourseChapter::getDeletedAt)
                        .orderByAsc(CourseChapter::getSort)
                        .orderByAsc(CourseChapter::getId));

        // 按章节顺序收集所有启用课程点
        List<CoursePoint> allPoints = new ArrayList<>();
        for (CourseChapter ch : chapters) {
            List<CoursePoint> points = coursePointMapper.selectList(
                    new LambdaQueryWrapper<CoursePoint>()
                            .eq(CoursePoint::getChapterId, ch.getId())
                            .eq(CoursePoint::getStatus, 1)
                            .isNull(CoursePoint::getDeletedAt)
                            .orderByAsc(CoursePoint::getSort)
                            .orderByAsc(CoursePoint::getId));
            allPoints.addAll(points);
        }

        // 查找当前课程点索引
        int currentIndex = -1;
        for (int i = 0; i < allPoints.size(); i++) {
            if (allPoints.get(i).getId().equals(pointId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex > 0) {
            nav.setPreviousPointId(allPoints.get(currentIndex - 1).getId());
        }
        if (currentIndex >= 0 && currentIndex < allPoints.size() - 1) {
            nav.setNextPointId(allPoints.get(currentIndex + 1).getId());
        }

        return nav;
    }
}
