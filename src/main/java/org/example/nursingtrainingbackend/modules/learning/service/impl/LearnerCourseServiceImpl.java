package org.example.nursingtrainingbackend.modules.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.category.entity.Category;
import org.example.nursingtrainingbackend.modules.category.mapper.CategoryMapper;
import org.example.nursingtrainingbackend.modules.course.entity.*;
import org.example.nursingtrainingbackend.modules.course.mapper.*;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.Article;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptMapper;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.Video;
import org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoMapper;
import org.example.nursingtrainingbackend.modules.learning.dto.LearnerCourseQuery;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCoursePointProgress;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCourseProgress;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCourseResourceProgress;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCoursePointProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCourseProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCourseResourceProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.service.LearnerCourseService;
import org.example.nursingtrainingbackend.modules.learning.vo.*;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 学员端课程列表服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearnerCourseServiceImpl implements LearnerCourseService {

    private static final String COURSE_STUDY_CACHE_PREFIX = "nursing:course:study:v1:";
    private static final long COURSE_STUDY_CACHE_TTL_MINUTES = 20;

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CategoryMapper categoryMapper;
    private final CourseDepartmentMapper courseDepartmentMapper;
    private final CourseChapterMapper courseChapterMapper;
    private final CoursePointMapper coursePointMapper;
    private final CoursePointArticleMapper coursePointArticleMapper;
    private final CoursePointVideoMapper coursePointVideoMapper;
    private final CoursePointPptMapper coursePointPptMapper;
    private final ArticleMapper articleMapper;
    private final VideoMapper videoMapper;
    private final PptMapper pptMapper;
    private final UserCourseProgressMapper userCourseProgressMapper;
    private final UserCoursePointProgressMapper userCoursePointProgressMapper;
    private final UserCourseResourceProgressMapper userCourseResourceProgressMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<LearnerCourseVO> getLearnerCourses(LearnerCourseQuery query) {
        Long userId = SecurityUtils.currentUserId();
        User user = validateLearner(userId);

        List<Long> visibleCourseIds = getVisibleCourseIds(user.getDeptId());
        if (visibleCourseIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L,
                    query.getPage().longValue(), query.getSize().longValue(), 0L);
        }

        // 批量获取课程信息
        Map<Long, Course> courseMap = loadCourseMap(visibleCourseIds);

        // 批量获取课程类型（必修/选修）
        Map<Long, Integer> courseTypeMap = loadCourseTypeMap(visibleCourseIds);

        // 批量获取进度
        Map<Long, UserCourseProgress> progressMap = loadProgressMap(visibleCourseIds, userId);

        // 批量获取课程点总数
        Map<Long, Integer> pointCountMap = loadPointCountMap(visibleCourseIds);

        // 批量获取已完成课程点数
        Map<Long, Integer> completedPointCountMap = loadCompletedPointCountMap(visibleCourseIds, userId);

        // 批量获取类别名称
        Map<Long, String> categoryNameMap = loadCategoryNameMap(courseMap.values());

        // 构建VO列表并过滤
        List<LearnerCourseVO> allCourses = visibleCourseIds.stream()
                .map(courseId -> buildCourseVO(courseId, courseMap, courseTypeMap, progressMap,
                        pointCountMap, completedPointCountMap, categoryNameMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 应用筛选条件
        List<LearnerCourseVO> filtered = applyFilters(allCourses, query);

        // 按更新时间倒序排序
        filtered.sort((a, b) -> {
            // 优先按学习状态排序：学习中 > 未开始 > 已完成
            int statusOrder1 = statusOrder(a.getLearningStatus());
            int statusOrder2 = statusOrder(b.getLearningStatus());
            if (statusOrder1 != statusOrder2) {
                return Integer.compare(statusOrder1, statusOrder2);
            }
            return Long.compare(b.getCourseId(), a.getCourseId());
        });

        // 分页
        int total = filtered.size();
        int fromIndex = (query.getPage() - 1) * query.getSize();
        int toIndex = Math.min(fromIndex + query.getSize(), total);

        if (fromIndex >= total) {
            return new PageResult<>(Collections.emptyList(), (long) total,
                    query.getPage().longValue(), query.getSize().longValue(),
                    (long) ((total + query.getSize() - 1) / query.getSize()));
        }

        List<LearnerCourseVO> pageRecords = filtered.subList(fromIndex, toIndex);
        long totalPages = (total + query.getSize() - 1) / query.getSize();

        return new PageResult<>(pageRecords, (long) total,
                query.getPage().longValue(), query.getSize().longValue(), totalPages);
    }

    @Override
    public CourseStatsVO getLearnerCourseStats() {
        Long userId = SecurityUtils.currentUserId();
        User user = validateLearner(userId);

        List<Long> visibleCourseIds = getVisibleCourseIds(user.getDeptId());

        CourseStatsVO stats = new CourseStatsVO();
        if (visibleCourseIds.isEmpty()) {
            stats.setAllCount(0);
            stats.setRequiredCount(0);
            stats.setOptionalCount(0);
            stats.setNotStartedCount(0);
            stats.setLearningCount(0);
            stats.setCompletedCount(0);
            stats.setRecommendedCount(0);
            return stats;
        }

        stats.setAllCount(visibleCourseIds.size());

        // 必修/选修
        Map<Long, Integer> courseTypeMap = loadCourseTypeMap(visibleCourseIds);
        long requiredCount = courseTypeMap.values().stream().filter(v -> v == 1).count();
        long optionalCount = courseTypeMap.values().stream().filter(v -> v == 0).count();
        stats.setRequiredCount((int) requiredCount);
        stats.setOptionalCount((int) optionalCount);

        // 学习状态
        Map<Long, UserCourseProgress> progressMap = loadProgressMap(visibleCourseIds, userId);
        long completedCount = progressMap.values().stream().filter(p -> p.getStatus() == 2).count();
        long learningCount = progressMap.values().stream().filter(p -> p.getStatus() == 1).count();
        long notStartedCount = visibleCourseIds.size() - completedCount - learningCount;
        stats.setCompletedCount((int) completedCount);
        stats.setLearningCount((int) learningCount);
        stats.setNotStartedCount((int) notStartedCount);

        // 推荐数量（未完成的可学习课程）
        long recommendedCount = visibleCourseIds.stream()
                .filter(id -> {
                    UserCourseProgress p = progressMap.get(id);
                    return p == null || p.getStatus() != 2;
                }).count();
        stats.setRecommendedCount((int) recommendedCount);

        return stats;
    }

    @Override
    @Transactional
    public StartLearningVO startLearning(Long courseId) {
        Long userId = SecurityUtils.currentUserId();
        User user = validateLearner(userId);

        // 1. 验证课程可见性
        Course course = validateCourseVisible(courseId, user.getDeptId());

        // 2. 获取第一个有效课程点
        CoursePoint firstPoint = getFirstValidCoursePoint(courseId);

        // 3. 查询或创建进度记录
        LambdaQueryWrapper<UserCourseProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseProgress::getUserId, userId)
               .eq(UserCourseProgress::getCourseId, courseId);
        UserCourseProgress progress = userCourseProgressMapper.selectOne(wrapper);

        LocalDateTime now = LocalDateTime.now();
        if (progress == null) {
            // 首次开始学习，创建进度记录
            progress = new UserCourseProgress();
            progress.setUserId(userId);
            progress.setCourseId(courseId);
            progress.setStatus(1); // LEARNING
            progress.setProgressPercent(BigDecimal.ZERO);
            progress.setLastPointId(firstPoint != null ? firstPoint.getId() : null);
            progress.setStartedAt(now);
            progress.setCreatedAt(now);
            progress.setUpdatedAt(now);
            userCourseProgressMapper.insert(progress);
        }

        // 4. 构建响应
        StartLearningVO vo = new StartLearningVO();
        vo.setCourseId(courseId);
        vo.setTitle(course.getTitle());

        if (progress.getStatus() == 2) {
            vo.setLearningStatus("COMPLETED");
            vo.setButtonText("复习课程");
        } else {
            vo.setLearningStatus("LEARNING");
            vo.setButtonText("继续学习");
        }

        Long currentPointId = progress.getLastPointId();
        if (currentPointId == null && firstPoint != null) {
            currentPointId = firstPoint.getId();
        }
        vo.setCurrentPointId(currentPointId);

        if (currentPointId != null) {
            CoursePoint point = coursePointMapper.selectById(currentPointId);
            vo.setCurrentPointTitle(point != null ? point.getTitle() : null);
        }

        return vo;
    }

    @Override
    public LearnerCourseDetailVO getCourseDetail(Long courseId) {
        Long userId = SecurityUtils.currentUserId();
        User user = validateLearner(userId);

        // 1. 验证课程可见性
        Course course = validateCourseVisible(courseId, user.getDeptId());

        // 2. 获取课程部门关系（必修/选修）
        LambdaQueryWrapper<CourseDepartment> cdWrapper = new LambdaQueryWrapper<>();
        cdWrapper.eq(CourseDepartment::getCourseId, courseId)
                 .eq(CourseDepartment::getDepartmentId, user.getDeptId());
        CourseDepartment courseDept = courseDepartmentMapper.selectOne(cdWrapper);

        // 3. 获取课程进度
        LambdaQueryWrapper<UserCourseProgress> progressWrapper = new LambdaQueryWrapper<>();
        progressWrapper.eq(UserCourseProgress::getUserId, userId)
                       .eq(UserCourseProgress::getCourseId, courseId);
        UserCourseProgress courseProgress = userCourseProgressMapper.selectOne(progressWrapper);

        // 4. 加载课程结构（优先从缓存）
        CourseStructureCacheVO structure = loadCourseStructure(courseId, course, user);

        // 5. 查询用户进度数据
        Map<Long, UserCoursePointProgress> pointProgressMap = loadPointProgressMap(courseId, userId, structure);
        Map<String, UserCourseResourceProgress> resourceProgressMap = loadResourceProgressMap(structure, userId);

        // 6. 组装响应：缓存结构 + 实时进度
        return buildDetailFromStructure(structure, courseDept, courseProgress,
                pointProgressMap, resourceProgressMap);
    }

    // ==================== 课程结构缓存 ====================

    /**
     * 加载课程结构：优先从 Redis 缓存读取，未命中则查询 DB 并写回缓存
     */
    private CourseStructureCacheVO loadCourseStructure(Long courseId, Course course, User user) {
        String cacheKey = COURSE_STUDY_CACHE_PREFIX + courseId;

        // 尝试从缓存读取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                return objectMapper.readValue(cachedJson, CourseStructureCacheVO.class);
            }
        } catch (Exception e) {
            log.warn("读取课程结构缓存失败, courseId={}", courseId, e);
        }

        // 缓存未命中，从 DB 构建
        CourseStructureCacheVO structure = buildCourseStructureFromDb(courseId, course, user);

        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(structure);
            redisTemplate.opsForValue().set(cacheKey, json, COURSE_STUDY_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入课程结构缓存失败, courseId={}", courseId, e);
        }

        return structure;
    }

    /**
     * 从 DB 构建课程结构缓存 VO（仅静态元数据，不含用户进度）
     */
    private CourseStructureCacheVO buildCourseStructureFromDb(Long courseId, Course course, User user) {
        // 类别名称
        String categoryName = null;
        if (course.getCategoryId() != null) {
            Category category = categoryMapper.selectById(course.getCategoryId());
            categoryName = category != null ? category.getName() : null;
        }

        // 查询章节列表
        LambdaQueryWrapper<CourseChapter> chapterWrapper = new LambdaQueryWrapper<>();
        chapterWrapper.eq(CourseChapter::getCourseId, courseId)
                      .isNull(CourseChapter::getDeletedAt)
                      .orderByAsc(CourseChapter::getSort);
        List<CourseChapter> chapters = courseChapterMapper.selectList(chapterWrapper);

        if (chapters.isEmpty()) {
            return CourseStructureCacheVO.builder()
                    .courseId(courseId)
                    .title(course.getTitle())
                    .summary(course.getSummary())
                    .coverUrl(course.getCoverUrl())
                    .categoryName(categoryName)
                    .totalPointCount(0)
                    .firstPointId(null)
                    .chapters(Collections.emptyList())
                    .build();
        }

        List<Long> chapterIds = chapters.stream().map(CourseChapter::getId).toList();

        // 查询所有启用课程点
        LambdaQueryWrapper<CoursePoint> pointWrapper = new LambdaQueryWrapper<>();
        pointWrapper.in(CoursePoint::getChapterId, chapterIds)
                    .eq(CoursePoint::getStatus, 1)
                    .isNull(CoursePoint::getDeletedAt)
                    .orderByAsc(CoursePoint::getSort);
        List<CoursePoint> allPoints = coursePointMapper.selectList(pointWrapper);
        List<Long> pointIds = allPoints.stream().map(CoursePoint::getId).toList();

        // 加载静态资源元数据（不含进度）
        Map<Long, List<CourseStructureCacheVO.ResourceItem>> pointResourcesMap = loadPointResourceMetadata(pointIds);

        // 构建章节结构
        Map<Long, List<CoursePoint>> pointsByChapter = allPoints.stream()
                .collect(Collectors.groupingBy(CoursePoint::getChapterId));

        List<CourseStructureCacheVO.ChapterItem> chapterItems = chapters.stream().map(chapter -> {
            List<CoursePoint> chapterPoints = pointsByChapter.getOrDefault(chapter.getId(), Collections.emptyList());
            List<CourseStructureCacheVO.PointItem> pointItems = chapterPoints.stream().map(point ->
                    CourseStructureCacheVO.PointItem.builder()
                            .pointId(point.getId())
                            .title(point.getTitle())
                            .description(point.getDescription())
                            .required(point.getRequired() != null && point.getRequired() == 1)
                            .sort(point.getSort())
                            .resources(pointResourcesMap.getOrDefault(point.getId(), Collections.emptyList()))
                            .build()
            ).collect(Collectors.toList());

            return CourseStructureCacheVO.ChapterItem.builder()
                    .chapterId(chapter.getId())
                    .title(chapter.getTitle())
                    .sort(chapter.getSort())
                    .points(pointItems)
                    .build();
        }).collect(Collectors.toList());

        Long firstPointId = allPoints.isEmpty() ? null : allPoints.get(0).getId();

        return CourseStructureCacheVO.builder()
                .courseId(courseId)
                .title(course.getTitle())
                .summary(course.getSummary())
                .coverUrl(course.getCoverUrl())
                .categoryName(categoryName)
                .totalPointCount(allPoints.size())
                .firstPointId(firstPointId)
                .chapters(chapterItems)
                .build();
    }

    /**
     * 批量加载课程点的静态资源元数据（标题、时长等，不含用户进度）
     */
    private Map<Long, List<CourseStructureCacheVO.ResourceItem>> loadPointResourceMetadata(List<Long> pointIds) {
        if (pointIds.isEmpty()) return Collections.emptyMap();

        Map<Long, List<CourseStructureCacheVO.ResourceItem>> result = new HashMap<>();

        // 文章
        List<CoursePointArticle> articleRels = coursePointArticleMapper.selectList(
                new LambdaQueryWrapper<CoursePointArticle>()
                        .in(CoursePointArticle::getCoursePointId, pointIds)
                        .orderByAsc(CoursePointArticle::getSort));
        Set<Long> articleIds = articleRels.stream().map(CoursePointArticle::getArticleId).collect(Collectors.toSet());
        Map<Long, Article> articleMap = articleIds.isEmpty() ? Collections.emptyMap() :
                articleMapper.selectList(new LambdaQueryWrapper<Article>().in(Article::getId, articleIds))
                        .stream().collect(Collectors.toMap(Article::getId, a -> a));
        for (CoursePointArticle rel : articleRels) {
            Article article = articleMap.get(rel.getArticleId());
            if (article == null) continue;
            result.computeIfAbsent(rel.getCoursePointId(), k -> new ArrayList<>())
                    .add(CourseStructureCacheVO.ResourceItem.builder()
                            .resourceType("ARTICLE").resourceId(article.getId())
                            .title(article.getTitle()).build());
        }

        // 视频
        List<CoursePointVideo> videoRels = coursePointVideoMapper.selectList(
                new LambdaQueryWrapper<CoursePointVideo>()
                        .in(CoursePointVideo::getCoursePointId, pointIds)
                        .orderByAsc(CoursePointVideo::getSort));
        Set<Long> videoIds = videoRels.stream().map(CoursePointVideo::getVideoId).collect(Collectors.toSet());
        Map<Long, Video> videoMap = videoIds.isEmpty() ? Collections.emptyMap() :
                videoMapper.selectList(new LambdaQueryWrapper<Video>().in(Video::getId, videoIds))
                        .stream().collect(Collectors.toMap(Video::getId, v -> v));
        for (CoursePointVideo rel : videoRels) {
            Video video = videoMap.get(rel.getVideoId());
            if (video == null) continue;
            result.computeIfAbsent(rel.getCoursePointId(), k -> new ArrayList<>())
                    .add(CourseStructureCacheVO.ResourceItem.builder()
                            .resourceType("VIDEO").resourceId(video.getId())
                            .title(video.getTitle()).durationSeconds(video.getDuration()).build());
        }

        // PPT
        List<CoursePointPpt> pptRels = coursePointPptMapper.selectList(
                new LambdaQueryWrapper<CoursePointPpt>()
                        .in(CoursePointPpt::getCoursePointId, pointIds)
                        .orderByAsc(CoursePointPpt::getSort));
        Set<Long> pptIds = pptRels.stream().map(CoursePointPpt::getPptId).collect(Collectors.toSet());
        Map<Long, Ppt> pptMap = pptIds.isEmpty() ? Collections.emptyMap() :
                pptMapper.selectList(new LambdaQueryWrapper<Ppt>().in(Ppt::getId, pptIds))
                        .stream().collect(Collectors.toMap(Ppt::getId, p -> p));
        for (CoursePointPpt rel : pptRels) {
            Ppt ppt = pptMap.get(rel.getPptId());
            if (ppt == null) continue;
            result.computeIfAbsent(rel.getCoursePointId(), k -> new ArrayList<>())
                    .add(CourseStructureCacheVO.ResourceItem.builder()
                            .resourceType("PPT").resourceId(ppt.getId())
                            .title(ppt.getTitle()).build());
        }

        return result;
    }

    // ==================== 进度数据加载 ====================

    /**
     * 批量加载课程点进度
     */
    private Map<Long, UserCoursePointProgress> loadPointProgressMap(Long courseId, Long userId, CourseStructureCacheVO structure) {
        List<Long> pointIds = new ArrayList<>();
        if (structure.getChapters() != null) {
            for (var ch : structure.getChapters()) {
                if (ch.getPoints() != null) {
                    ch.getPoints().forEach(p -> pointIds.add(p.getPointId()));
                }
            }
        }
        if (pointIds.isEmpty()) return Collections.emptyMap();

        LambdaQueryWrapper<UserCoursePointProgress> ppWrapper = new LambdaQueryWrapper<>();
        ppWrapper.eq(UserCoursePointProgress::getUserId, userId)
                 .in(UserCoursePointProgress::getCoursePointId, pointIds);
        return userCoursePointProgressMapper.selectList(ppWrapper).stream()
                .collect(Collectors.toMap(UserCoursePointProgress::getCoursePointId, p -> p, (p1, p2) -> p1));
    }

    /**
     * 批量加载资源进度，key="TYPE:resourceId:coursePointId"
     */
    private Map<String, UserCourseResourceProgress> loadResourceProgressMap(CourseStructureCacheVO structure, Long userId) {
        List<Long> pointIds = new ArrayList<>();
        if (structure.getChapters() != null) {
            for (var ch : structure.getChapters()) {
                if (ch.getPoints() != null) {
                    ch.getPoints().forEach(p -> pointIds.add(p.getPointId()));
                }
            }
        }
        if (pointIds.isEmpty()) return Collections.emptyMap();

        LambdaQueryWrapper<UserCourseResourceProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseResourceProgress::getUserId, userId)
               .in(UserCourseResourceProgress::getCoursePointId, pointIds);
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

    // ==================== 响应组装 ====================

    /**
     * 用缓存的课程结构 + 实时进度数据组装 LearnerCourseDetailVO
     */
    private LearnerCourseDetailVO buildDetailFromStructure(CourseStructureCacheVO structure,
                                                            CourseDepartment courseDept,
                                                            UserCourseProgress courseProgress,
                                                            Map<Long, UserCoursePointProgress> pointProgressMap,
                                                            Map<String, UserCourseResourceProgress> resourceProgressMap) {
        LearnerCourseDetailVO vo = new LearnerCourseDetailVO();
        vo.setCourseId(structure.getCourseId());
        vo.setTitle(structure.getTitle());
        vo.setSummary(structure.getSummary());
        vo.setCoverUrl(structure.getCoverUrl());
        vo.setCategoryName(structure.getCategoryName());
        vo.setCourseType(courseDept != null && courseDept.getRequired() != null && courseDept.getRequired() == 1 ? "REQUIRED" : "OPTIONAL");
        vo.setTotalPointCount(structure.getTotalPointCount());

        // 统计已完成课程点
        int completedPointCount = (int) pointProgressMap.values().stream()
                .filter(p -> p.getStatus() == 2).count();
        vo.setCompletedPointCount(completedPointCount);

        // 学习状态
        if (courseProgress == null) {
            vo.setLearningStatus("NOT_STARTED");
            vo.setProgressPercent(BigDecimal.ZERO);
            vo.setButtonText("开始学习");
        } else if (courseProgress.getStatus() == 1) {
            vo.setLearningStatus("LEARNING");
            vo.setProgressPercent(courseProgress.getProgressPercent() != null ? courseProgress.getProgressPercent() : BigDecimal.ZERO);
            vo.setButtonText("继续学习");
        } else {
            vo.setLearningStatus("COMPLETED");
            vo.setProgressPercent(BigDecimal.valueOf(100));
            vo.setButtonText("复习课程");
        }

        // 当前学习课程点
        Long currentPointId = courseProgress != null ? courseProgress.getLastPointId() : null;
        if (currentPointId == null && structure.getFirstPointId() != null) {
            currentPointId = structure.getFirstPointId();
        }
        vo.setCurrentPointId(currentPointId);

        // 构建章节+课程点结构（含实时进度）
        if (structure.getChapters() != null) {
            List<LearnerCourseDetailVO.ChapterVO> chapterVOs = structure.getChapters().stream().map(cachedChapter -> {
                LearnerCourseDetailVO.ChapterVO chapterVO = new LearnerCourseDetailVO.ChapterVO();
                chapterVO.setChapterId(cachedChapter.getChapterId());
                chapterVO.setTitle(cachedChapter.getTitle());
                chapterVO.setSort(cachedChapter.getSort());

                List<LearnerCourseDetailVO.PointVO> pointVOs = cachedChapter.getPoints() != null
                        ? cachedChapter.getPoints().stream().map(cachedPoint -> {
                    LearnerCourseDetailVO.PointVO pointVO = new LearnerCourseDetailVO.PointVO();
                    pointVO.setPointId(cachedPoint.getPointId());
                    pointVO.setTitle(cachedPoint.getTitle());
                    pointVO.setDescription(cachedPoint.getDescription());
                    pointVO.setRequired(cachedPoint.getRequired());
                    pointVO.setSort(cachedPoint.getSort());

                    // 课程点进度
                    UserCoursePointProgress pp = pointProgressMap.get(cachedPoint.getPointId());
                    if (pp == null) {
                        pointVO.setLearningStatus("NOT_STARTED");
                    } else if (pp.getStatus() == 1) {
                        pointVO.setLearningStatus("LEARNING");
                    } else {
                        pointVO.setLearningStatus("COMPLETED");
                    }

                    // 资源列表（含实时进度）
                    List<LearnerCourseDetailVO.ResourceVO> resources = cachedPoint.getResources() != null
                            ? cachedPoint.getResources().stream().map(cachedRes -> {
                        LearnerCourseDetailVO.ResourceVO resVO = new LearnerCourseDetailVO.ResourceVO();
                        resVO.setResourceType(cachedRes.getResourceType());
                        resVO.setResourceId(cachedRes.getResourceId());
                        resVO.setTitle(cachedRes.getTitle());
                        resVO.setDurationSeconds(cachedRes.getDurationSeconds());

                        String key = cachedRes.getResourceType() + ":" + cachedRes.getResourceId() + ":" + cachedPoint.getPointId();
                        UserCourseResourceProgress rp = resourceProgressMap.get(key);
                        if (rp == null) {
                            resVO.setLearningStatus("NOT_STARTED");
                            resVO.setProgressPercent(BigDecimal.ZERO);
                            if ("VIDEO".equals(cachedRes.getResourceType())) resVO.setLastPositionSeconds(0);
                        } else if (rp.getStatus() == 1) {
                            resVO.setLearningStatus("LEARNING");
                            resVO.setProgressPercent(rp.getProgressPercent() != null ? rp.getProgressPercent() : BigDecimal.ZERO);
                            if ("VIDEO".equals(cachedRes.getResourceType())) {
                                resVO.setLastPositionSeconds(rp.getLastPositionSeconds() != null ? rp.getLastPositionSeconds() : 0);
                            }
                        } else {
                            resVO.setLearningStatus("COMPLETED");
                            resVO.setProgressPercent(BigDecimal.valueOf(100));
                            if ("VIDEO".equals(cachedRes.getResourceType())) {
                                resVO.setLastPositionSeconds(cachedRes.getDurationSeconds());
                            }
                        }
                        return resVO;
                    }).collect(Collectors.toList())
                            : Collections.emptyList();
                    pointVO.setResources(resources);
                    return pointVO;
                }).collect(Collectors.toList())
                        : Collections.emptyList();

                chapterVO.setPoints(pointVOs);
                return chapterVO;
            }).collect(Collectors.toList());
            vo.setChapters(chapterVOs);
        } else {
            vo.setChapters(Collections.emptyList());
        }

        return vo;
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

    private List<Long> getVisibleCourseIds(Long deptId) {
        LambdaQueryWrapper<CourseDepartment> cdWrapper = new LambdaQueryWrapper<>();
        cdWrapper.eq(CourseDepartment::getDepartmentId, deptId);
        List<CourseDepartment> courseDepartments = courseDepartmentMapper.selectList(cdWrapper);

        if (courseDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> courseIds = courseDepartments.stream()
                .map(CourseDepartment::getCourseId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.in(Course::getId, courseIds)
                     .eq(Course::getStatus, 1)
                     .isNull(Course::getDeletedAt);

        return courseMapper.selectList(courseWrapper).stream()
                .map(Course::getId)
                .collect(Collectors.toList());
    }

    private Map<Long, Course> loadCourseMap(List<Long> courseIds) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Course::getId, courseIds);
        return courseMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
    }

    private Map<Long, Integer> loadCourseTypeMap(List<Long> courseIds) {
        LambdaQueryWrapper<CourseDepartment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CourseDepartment::getCourseId, courseIds);
        return courseDepartmentMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(CourseDepartment::getCourseId, CourseDepartment::getRequired, (v1, v2) -> v1));
    }

    private Map<Long, UserCourseProgress> loadProgressMap(List<Long> courseIds, Long userId) {
        LambdaQueryWrapper<UserCourseProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseProgress::getUserId, userId)
               .in(UserCourseProgress::getCourseId, courseIds);
        return userCourseProgressMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(UserCourseProgress::getCourseId, p -> p, (p1, p2) -> p1));
    }

    private Map<Long, Integer> loadPointCountMap(List<Long> courseIds) {
        LambdaQueryWrapper<CoursePoint> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CoursePoint::getCourseId, courseIds)
               .eq(CoursePoint::getStatus, 1)
               .isNull(CoursePoint::getDeletedAt);
        List<CoursePoint> points = coursePointMapper.selectList(wrapper);
        return points.stream()
                .collect(Collectors.groupingBy(CoursePoint::getCourseId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    private Map<Long, Integer> loadCompletedPointCountMap(List<Long> courseIds, Long userId) {
        LambdaQueryWrapper<UserCoursePointProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoursePointProgress::getUserId, userId)
               .in(UserCoursePointProgress::getCourseId, courseIds)
               .eq(UserCoursePointProgress::getStatus, 2);
        List<UserCoursePointProgress> completed = userCoursePointProgressMapper.selectList(wrapper);
        return completed.stream()
                .collect(Collectors.groupingBy(UserCoursePointProgress::getCourseId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    private Map<Long, String> loadCategoryNameMap(Collection<Course> courses) {
        Set<Long> categoryIds = courses.stream()
                .map(Course::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Category::getId, categoryIds);
        return categoryMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private LearnerCourseVO buildCourseVO(Long courseId,
                                           Map<Long, Course> courseMap,
                                           Map<Long, Integer> courseTypeMap,
                                           Map<Long, UserCourseProgress> progressMap,
                                           Map<Long, Integer> pointCountMap,
                                           Map<Long, Integer> completedPointCountMap,
                                           Map<Long, String> categoryNameMap) {
        Course course = courseMap.get(courseId);
        if (course == null) return null;

        LearnerCourseVO vo = new LearnerCourseVO();
        vo.setCourseId(courseId);
        vo.setTitle(course.getTitle());
        vo.setSummary(course.getSummary());
        vo.setCoverUrl(course.getCoverUrl());
        vo.setInstructorName(null); // TODO: 讲师姓名

        vo.setCategoryName(categoryNameMap.get(course.getCategoryId()));

        Integer required = courseTypeMap.get(courseId);
        vo.setCourseType(required != null && required == 1 ? "REQUIRED" : "OPTIONAL");

        vo.setPointCount(pointCountMap.getOrDefault(courseId, 0));
        vo.setCompletedPointCount(completedPointCountMap.getOrDefault(courseId, 0));

        UserCourseProgress progress = progressMap.get(courseId);
        if (progress == null) {
            vo.setLearningStatus("NOT_STARTED");
            vo.setProgressPercent(BigDecimal.ZERO);
            vo.setButtonText("开始学习");
            vo.setLastLearnedAt(null);
        } else if (progress.getStatus() == 1) {
            vo.setLearningStatus("LEARNING");
            vo.setProgressPercent(progress.getProgressPercent() != null ? progress.getProgressPercent() : BigDecimal.ZERO);
            vo.setButtonText("继续学习");
            vo.setLastLearnedAt(progress.getUpdatedAt());
        } else {
            vo.setLearningStatus("COMPLETED");
            vo.setProgressPercent(progress.getProgressPercent() != null ? progress.getProgressPercent() : BigDecimal.valueOf(100));
            vo.setButtonText("复习课程");
            vo.setLastLearnedAt(progress.getCompletedAt() != null ? progress.getCompletedAt() : progress.getUpdatedAt());
        }

        return vo;
    }

    private List<LearnerCourseVO> applyFilters(List<LearnerCourseVO> courses, LearnerCourseQuery query) {
        return courses.stream()
                .filter(c -> {
                    // 学习状态筛选
                    if (StringUtils.hasText(query.getLearningStatus()) && !"ALL".equals(query.getLearningStatus())) {
                        if (!query.getLearningStatus().equals(c.getLearningStatus())) {
                            return false;
                        }
                    }
                    // 课程性质筛选
                    if (StringUtils.hasText(query.getCourseType()) && !"ALL".equals(query.getCourseType())) {
                        if (!query.getCourseType().equals(c.getCourseType())) {
                            return false;
                        }
                    }
                    // 关键词筛选
                    if (StringUtils.hasText(query.getKeyword())) {
                        String kw = query.getKeyword().toLowerCase();
                        boolean titleMatch = c.getTitle() != null && c.getTitle().toLowerCase().contains(kw);
                        boolean instructorMatch = c.getInstructorName() != null && c.getInstructorName().toLowerCase().contains(kw);
                        if (!titleMatch && !instructorMatch) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private int statusOrder(String status) {
        return switch (status) {
            case "LEARNING" -> 0;
            case "NOT_STARTED" -> 1;
            case "COMPLETED" -> 2;
            default -> 3;
        };
    }

    /**
     * 验证课程对学员可见且已发布
     */
    private Course validateCourseVisible(Long courseId, Long deptId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.LEARNER_COURSE_NOT_VISIBLE);
        }
        if (course.getStatus() != 1) {
            throw new BusinessException(ErrorCode.LEARNER_COURSE_NOT_PUBLISHED);
        }

        // 验证课程是否分配给学员部门
        LambdaQueryWrapper<CourseDepartment> cdWrapper = new LambdaQueryWrapper<>();
        cdWrapper.eq(CourseDepartment::getCourseId, courseId)
                 .eq(CourseDepartment::getDepartmentId, deptId);
        Long count = courseDepartmentMapper.selectCount(cdWrapper);
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.LEARNER_COURSE_NOT_VISIBLE);
        }

        return course;
    }

    /**
     * 获取课程的第一个有效课程点
     */
    private CoursePoint getFirstValidCoursePoint(Long courseId) {
        LambdaQueryWrapper<CoursePoint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoursePoint::getCourseId, courseId)
               .eq(CoursePoint::getStatus, 1)
               .isNull(CoursePoint::getDeletedAt)
               .orderByAsc(CoursePoint::getSort)
               .last("LIMIT 1");
        return coursePointMapper.selectOne(wrapper);
    }

    /**
     * 构建空的课程详情（无章节时）
     */
    private LearnerCourseDetailVO buildEmptyDetail(Course course, String categoryName,
                                                    CourseDepartment courseDept,
                                                    UserCourseProgress courseProgress) {
        LearnerCourseDetailVO vo = new LearnerCourseDetailVO();
        vo.setCourseId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setSummary(course.getSummary());
        vo.setCoverUrl(course.getCoverUrl());
        vo.setCategoryName(categoryName);
        vo.setCourseType(courseDept != null && courseDept.getRequired() != null && courseDept.getRequired() == 1 ? "REQUIRED" : "OPTIONAL");
        vo.setTotalPointCount(0);
        vo.setCompletedPointCount(0);
        vo.setChapters(Collections.emptyList());

        if (courseProgress == null) {
            vo.setLearningStatus("NOT_STARTED");
            vo.setProgressPercent(BigDecimal.ZERO);
            vo.setButtonText("开始学习");
        } else if (courseProgress.getStatus() == 1) {
            vo.setLearningStatus("LEARNING");
            vo.setProgressPercent(courseProgress.getProgressPercent() != null ? courseProgress.getProgressPercent() : BigDecimal.ZERO);
            vo.setButtonText("继续学习");
        } else {
            vo.setLearningStatus("COMPLETED");
            vo.setProgressPercent(BigDecimal.valueOf(100));
            vo.setButtonText("复习课程");
        }

        return vo;
    }

    /**
     * 批量加载课程点的课件信息和学员课件进度
     */
    private Map<Long, List<LearnerCourseDetailVO.ResourceVO>> loadPointResources(List<Long> pointIds, Long userId) {
        if (pointIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<LearnerCourseDetailVO.ResourceVO>> result = new HashMap<>();

        // 1. 查询课件关联关系
        LambdaQueryWrapper<CoursePointArticle> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.in(CoursePointArticle::getCoursePointId, pointIds)
                      .orderByAsc(CoursePointArticle::getSort);
        List<CoursePointArticle> articleRelations = coursePointArticleMapper.selectList(articleWrapper);

        LambdaQueryWrapper<CoursePointVideo> videoWrapper = new LambdaQueryWrapper<>();
        videoWrapper.in(CoursePointVideo::getCoursePointId, pointIds)
                    .orderByAsc(CoursePointVideo::getSort);
        List<CoursePointVideo> videoRelations = coursePointVideoMapper.selectList(videoWrapper);

        LambdaQueryWrapper<CoursePointPpt> pptWrapper = new LambdaQueryWrapper<>();
        pptWrapper.in(CoursePointPpt::getCoursePointId, pointIds)
                  .orderByAsc(CoursePointPpt::getSort);
        List<CoursePointPpt> pptRelations = coursePointPptMapper.selectList(pptWrapper);

        // 2. 批量查询课件标题
        Set<Long> articleIds = articleRelations.stream().map(CoursePointArticle::getArticleId).collect(Collectors.toSet());
        Set<Long> videoIds = videoRelations.stream().map(CoursePointVideo::getVideoId).collect(Collectors.toSet());
        Set<Long> pptIds = pptRelations.stream().map(CoursePointPpt::getPptId).collect(Collectors.toSet());

        Map<Long, Article> articleMap = Collections.emptyMap();
        if (!articleIds.isEmpty()) {
            LambdaQueryWrapper<Article> aWrapper = new LambdaQueryWrapper<>();
            aWrapper.in(Article::getId, articleIds);
            articleMap = articleMapper.selectList(aWrapper).stream()
                    .collect(Collectors.toMap(Article::getId, a -> a));
        }

        Map<Long, Video> videoMap = Collections.emptyMap();
        if (!videoIds.isEmpty()) {
            LambdaQueryWrapper<Video> vWrapper = new LambdaQueryWrapper<>();
            vWrapper.in(Video::getId, videoIds);
            videoMap = videoMapper.selectList(vWrapper).stream()
                    .collect(Collectors.toMap(Video::getId, v -> v));
        }

        Map<Long, Ppt> pptMap = Collections.emptyMap();
        if (!pptIds.isEmpty()) {
            LambdaQueryWrapper<Ppt> pWrapper = new LambdaQueryWrapper<>();
            pWrapper.in(Ppt::getId, pptIds);
            pptMap = pptMapper.selectList(pWrapper).stream()
                    .collect(Collectors.toMap(Ppt::getId, p -> p));
        }

        // 3. 批量查询课件进度
        // 构建 (resourceType, resourceId) -> progress 的映射
        Map<String, UserCourseResourceProgress> resourceProgressMap = loadResourceProgressMap(pointIds, userId);

        // 4. 组装每个课程点的课件列表
        for (CoursePointArticle rel : articleRelations) {
            Article article = articleMap.get(rel.getArticleId());
            if (article == null) continue;

            LearnerCourseDetailVO.ResourceVO res = new LearnerCourseDetailVO.ResourceVO();
            res.setResourceType("ARTICLE");
            res.setResourceId(article.getId());
            res.setTitle(article.getTitle());

            String key = "ARTICLE:" + article.getId() + ":" + rel.getCoursePointId();
            UserCourseResourceProgress rp = resourceProgressMap.get(key);
            if (rp == null) {
                res.setLearningStatus("NOT_STARTED");
                res.setProgressPercent(BigDecimal.ZERO);
            } else if (rp.getStatus() == 1) {
                res.setLearningStatus("LEARNING");
                res.setProgressPercent(rp.getProgressPercent() != null ? rp.getProgressPercent() : BigDecimal.ZERO);
            } else {
                res.setLearningStatus("COMPLETED");
                res.setProgressPercent(BigDecimal.valueOf(100));
            }

            result.computeIfAbsent(rel.getCoursePointId(), k -> new ArrayList<>()).add(res);
        }

        for (CoursePointVideo rel : videoRelations) {
            Video video = videoMap.get(rel.getVideoId());
            if (video == null) continue;

            LearnerCourseDetailVO.ResourceVO res = new LearnerCourseDetailVO.ResourceVO();
            res.setResourceType("VIDEO");
            res.setResourceId(video.getId());
            res.setTitle(video.getTitle());
            res.setDurationSeconds(video.getDuration());

            String key = "VIDEO:" + video.getId() + ":" + rel.getCoursePointId();
            UserCourseResourceProgress rp = resourceProgressMap.get(key);
            if (rp == null) {
                res.setLearningStatus("NOT_STARTED");
                res.setProgressPercent(BigDecimal.ZERO);
                res.setLastPositionSeconds(0);
            } else if (rp.getStatus() == 1) {
                res.setLearningStatus("LEARNING");
                res.setProgressPercent(rp.getProgressPercent() != null ? rp.getProgressPercent() : BigDecimal.ZERO);
                res.setLastPositionSeconds(rp.getLastPositionSeconds() != null ? rp.getLastPositionSeconds() : 0);
            } else {
                res.setLearningStatus("COMPLETED");
                res.setProgressPercent(BigDecimal.valueOf(100));
                res.setLastPositionSeconds(video.getDuration());
            }

            result.computeIfAbsent(rel.getCoursePointId(), k -> new ArrayList<>()).add(res);
        }

        for (CoursePointPpt rel : pptRelations) {
            Ppt ppt = pptMap.get(rel.getPptId());
            if (ppt == null) continue;

            LearnerCourseDetailVO.ResourceVO res = new LearnerCourseDetailVO.ResourceVO();
            res.setResourceType("PPT");
            res.setResourceId(ppt.getId());
            res.setTitle(ppt.getTitle());

            String key = "PPT:" + ppt.getId() + ":" + rel.getCoursePointId();
            UserCourseResourceProgress rp = resourceProgressMap.get(key);
            if (rp == null) {
                res.setLearningStatus("NOT_STARTED");
                res.setProgressPercent(BigDecimal.ZERO);
            } else if (rp.getStatus() == 1) {
                res.setLearningStatus("LEARNING");
                res.setProgressPercent(rp.getProgressPercent() != null ? rp.getProgressPercent() : BigDecimal.ZERO);
            } else {
                res.setLearningStatus("COMPLETED");
                res.setProgressPercent(BigDecimal.valueOf(100));
            }

            result.computeIfAbsent(rel.getCoursePointId(), k -> new ArrayList<>()).add(res);
        }

        return result;
    }

    /**
     * 批量查询课件进度，返回 key="TYPE:resourceId:coursePointId" -> progress 的映射
     */
    private Map<String, UserCourseResourceProgress> loadResourceProgressMap(List<Long> pointIds, Long userId) {
        LambdaQueryWrapper<UserCourseResourceProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseResourceProgress::getUserId, userId)
               .in(UserCourseResourceProgress::getCoursePointId, pointIds);
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
}
