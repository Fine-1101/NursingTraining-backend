package org.example.nursingtrainingbackend.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.example.nursingtrainingbackend.common.event.CacheEvictionEvent;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.category.entity.Category;
import org.example.nursingtrainingbackend.modules.category.mapper.CategoryMapper;
import org.example.nursingtrainingbackend.modules.course.dto.ExportCourseDTO;
import org.example.nursingtrainingbackend.modules.course.dto.GetCourseDTO;
import org.example.nursingtrainingbackend.modules.course.dto.CourseStatusDTO;
import org.example.nursingtrainingbackend.modules.course.entity.*;
import org.example.nursingtrainingbackend.modules.course.mapper.*;
import org.example.nursingtrainingbackend.modules.course.service.CourseManagementService;
import org.example.nursingtrainingbackend.modules.course.vo.*;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.Article;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptMapper;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.Video;
import org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoMapper;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class CourseManagementServiceImpl implements CourseManagementService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CourseStatSnapshotMapper snapshotMapper;

    @Autowired
    private CourseChapterMapper courseChapterMapper;

    @Autowired
    private CoursePointMapper coursePointMapper;

    @Autowired
    private CoursePointArticleMapper coursePointArticleMapper;

    @Autowired
    private CoursePointVideoMapper coursePointVideoMapper;

    @Autowired
    private CoursePointPptMapper coursePointPptMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private PptMapper pptMapper;

    @Autowired
    private CourseDepartmentMapper courseDepartmentMapper;

    @Autowired
    private CourseTagMapper courseTagMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<CourseItemVO> getCourses(GetCourseDTO dto) {
        // 1. keyword 去首尾空格
        String keyword = null;
        if (dto.getKeyword() != null) {
            keyword = dto.getKeyword().trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

        // 2. categoryId 校验：必须存在且未删除
        if (dto.getCategoryId() != null) {
            Category category = categoryMapper.selectOne(
                    new LambdaQueryWrapper<Category>()
                            .eq(Category::getId, dto.getCategoryId())
            );
            if (category == null) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }

        // 3. status 字符串转整数
        Integer statusCode = null;
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            statusCode = switch (dto.getStatus()) {
                case "DRAFT" -> 0;
                case "PUBLISHED" -> 1;
                case "OFFLINE" -> 2;
                default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "状态仅支持 DRAFT、PUBLISHED、OFFLINE");
            };
        }

        // 4. 分页查询
        Page<CourseItemVO> page = new Page<>(dto.getPage(), dto.getSize());
        var result = courseMapper.selectCoursePage(page, keyword, dto.getCategoryId(), statusCode);

        return PageResult.from(result);
    }

    @Override
    public CourseOverviewVO getCourseOverview() {
        // 1. 实时统计各状态课程数
        long draftCount = courseMapper.selectCount(
                new LambdaQueryWrapper<Course>().eq(Course::getStatus, 0));
        long publishedCount = courseMapper.selectCount(
                new LambdaQueryWrapper<Course>().eq(Course::getStatus, 1));
        long offlineCount = courseMapper.selectCount(
                new LambdaQueryWrapper<Course>().eq(Course::getStatus, 2));
        long totalCount = draftCount + publishedCount + offlineCount;

        // 2. 查询上月月末快照
        LocalDate lastMonthEnd = YearMonth.now().minusMonths(1).atEndOfMonth();
        CourseStatSnapshot snapshot = snapshotMapper.selectOne(
                new LambdaQueryWrapper<CourseStatSnapshot>()
                        .eq(CourseStatSnapshot::getStatDate, lastMonthEnd));

        // 3. 构建响应
        CourseOverviewVO vo = new CourseOverviewVO();
        vo.setTotal(buildStatItem(totalCount, snapshot != null ? snapshot.getTotalCourses() : null));
        vo.setDraft(buildStatItem(draftCount, snapshot != null ? snapshot.getDraftCourses() : null));
        vo.setPublished(buildStatItem(publishedCount, snapshot != null ? snapshot.getPublishedCourses() : null));
        vo.setOffline(buildStatItem(offlineCount, snapshot != null ? snapshot.getOfflineCourses() : null));
        vo.setComparisonDate(snapshot != null ? snapshot.getStatDate() : null);

        return vo;
    }

    @Override
    public CourseDetailVO getCoursePreview(Long courseId) {
        // 1. 查询课程（草稿、已发布、已下架均可预览）
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        // 2. 查询讲师姓名
        String instructorName = null;
        if (course.getInstructorId() != null) {
            User instructor = userMapper.selectById(course.getInstructorId());
            if (instructor != null) {
                instructorName = instructor.getRealName();
            }
        }

        // 3. 构建 VO
        CourseDetailVO vo = new CourseDetailVO();
        vo.setCourseId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setSummary(course.getSummary());
        vo.setCoverUrl(course.getCoverUrl());
        vo.setInstructorName(instructorName);
        vo.setStatus(convertPreviewStatus(course.getStatus()));

        // 4. 查询启用且未删除的章节
        List<CourseChapter> chapters = courseChapterMapper.selectList(
                Wrappers.<CourseChapter>lambdaQuery()
                        .eq(CourseChapter::getCourseId, courseId)
                        .eq(CourseChapter::getStatus, 1)
                        .orderByAsc(CourseChapter::getSort));

        List<CourseDetailVO.ChapterVO> chapterVOs = chapters.stream().map(chapter -> {
            CourseDetailVO.ChapterVO chapterVO = new CourseDetailVO.ChapterVO();
            chapterVO.setId(chapter.getId());
            chapterVO.setTitle(chapter.getTitle());
            chapterVO.setSort(chapter.getSort());

            // 5. 查询该章节下启用且未删除的课程点
            List<CoursePoint> points = coursePointMapper.selectList(
                    Wrappers.<CoursePoint>lambdaQuery()
                            .eq(CoursePoint::getChapterId, chapter.getId())
                            .eq(CoursePoint::getStatus, 1)
                            .orderByAsc(CoursePoint::getSort));

            List<CourseDetailVO.PointVO> pointVOs = points.stream().map(point -> {
                CourseDetailVO.PointVO pointVO = new CourseDetailVO.PointVO();
                pointVO.setId(point.getId());
                pointVO.setTitle(point.getTitle());
                pointVO.setDescription(point.getDescription());
                pointVO.setRequired(point.getRequired() != null && point.getRequired() == 1);
                pointVO.setSort(point.getSort());

                // 6. 查询已关联的已发布文章摘要
                List<CoursePointArticle> cpas = coursePointArticleMapper.selectList(
                        Wrappers.<CoursePointArticle>lambdaQuery()
                                .eq(CoursePointArticle::getCoursePointId, point.getId())
                                .orderByAsc(CoursePointArticle::getSort));
                List<CourseDetailVO.ArticleSummaryVO> articleSummaries = cpas.stream()
                        .map(cpa -> {
                            Article article = articleMapper.selectById(cpa.getArticleId());
                            if (article != null && Integer.valueOf(1).equals(article.getStatus())) {
                                CourseDetailVO.ArticleSummaryVO summary = new CourseDetailVO.ArticleSummaryVO();
                                summary.setId(article.getId());
                                summary.setTitle(article.getTitle());
                                summary.setCoverUrl(article.getCoverUrl());
                                return summary;
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .toList();
                pointVO.setArticles(articleSummaries);

                // 7. 查询已关联的已发布视频摘要
                List<CoursePointVideo> cpvs = coursePointVideoMapper.selectList(
                        Wrappers.<CoursePointVideo>lambdaQuery()
                                .eq(CoursePointVideo::getCoursePointId, point.getId())
                                .orderByAsc(CoursePointVideo::getSort));
                List<CourseDetailVO.VideoSummaryVO> videoSummaries = cpvs.stream()
                        .map(cpv -> {
                            Video video = videoMapper.selectById(cpv.getVideoId());
                            if (video != null && Integer.valueOf(1).equals(video.getStatus())) {
                                CourseDetailVO.VideoSummaryVO summary = new CourseDetailVO.VideoSummaryVO();
                                summary.setId(video.getId());
                                summary.setTitle(video.getTitle());
                                summary.setCoverUrl(video.getCoverUrl());
                                summary.setDuration(video.getDuration());
                                return summary;
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .toList();
                pointVO.setVideos(videoSummaries);

                // 8. 查询已关联的已发布 PPT 摘要
                List<CoursePointPpt> cpps = coursePointPptMapper.selectList(
                        Wrappers.<CoursePointPpt>lambdaQuery()
                                .eq(CoursePointPpt::getCoursePointId, point.getId())
                                .orderByAsc(CoursePointPpt::getSort));
                List<CourseDetailVO.PptSummaryVO> pptSummaries = cpps.stream()
                        .map(cpp -> {
                            Ppt ppt = pptMapper.selectById(cpp.getPptId());
                            if (ppt != null && Integer.valueOf(1).equals(ppt.getStatus())) {
                                CourseDetailVO.PptSummaryVO summary = new CourseDetailVO.PptSummaryVO();
                                summary.setId(ppt.getId());
                                summary.setTitle(ppt.getTitle());
                                summary.setCoverUrl(ppt.getCoverUrl());
                                return summary;
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .toList();
                pointVO.setPpts(pptSummaries);

                // 设置课件数量（兼容已有字段）
                pointVO.setArticleCount(articleSummaries.size());
                pointVO.setVideoCount(videoSummaries.size());
                pointVO.setPptCount(pptSummaries.size());

                return pointVO;
            }).toList();

            chapterVO.setPoints(pointVOs);
            return chapterVO;
        }).toList();

        vo.setChapters(chapterVOs);
        return vo;
    }

    private String convertPreviewStatus(Integer status) {
        if (status == null) return "DRAFT";
        return switch (status) {
            case 0 -> "DRAFT";
            case 1 -> "PUBLISHED";
            case 2 -> "OFFLINE";
            default -> "UNKNOWN";
        };
    }

    // ==================== 课程导出 ====================

    private static final String[] EXPORT_HEADERS = {"课程名称", "类别", "讲师", "学员数", "发布状态", "更新时间"};
    private static final DateTimeFormatter EXPORT_FILENAME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public void exportCourses(ExportCourseDTO dto, HttpServletResponse response) {
        // 1. 参数处理（与分页查询口径一致）
        String keyword = null;
        if (dto.getKeyword() != null) {
            keyword = dto.getKeyword().trim();
            if (keyword.isEmpty()) keyword = null;
        }

        if (dto.getCategoryId() != null) {
            Category category = categoryMapper.selectOne(
                    new LambdaQueryWrapper<Category>().eq(Category::getId, dto.getCategoryId()));
            if (category == null) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }

        Integer statusCode = null;
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            statusCode = switch (dto.getStatus()) {
                case "DRAFT" -> 0;
                case "PUBLISHED" -> 1;
                case "OFFLINE" -> 2;
                default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "导出条件不合法");
            };
        }

        // 2. 查询全部匹配记录
        List<CourseExportRowVO> rows = courseMapper.selectCourseExport(keyword, dto.getCategoryId(), statusCode);

        // 3. 生成文件名
        String timestamp = LocalDateTime.now().format(EXPORT_FILENAME_FMT);
        String rawFilename = "courses-" + timestamp + ".xlsx";
        String encodedFilename = URLEncoder.encode(rawFilename, StandardCharsets.UTF_8).replace("+", "%20");

        // 4. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        // 5. 流式写入 XLSX
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(200)) {
            Sheet sheet = workbook.createSheet("课程列表");

            // 表头
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowIdx = 1;
            for (CourseExportRowVO row : rows) {
                Row dataRow = sheet.createRow(rowIdx++);
                dataRow.createCell(0).setCellValue(row.getTitle() != null ? row.getTitle() : "");
                dataRow.createCell(1).setCellValue(row.getCategoryName() != null ? row.getCategoryName() : "");
                dataRow.createCell(2).setCellValue(row.getInstructorName() != null ? row.getInstructorName() : "");
                dataRow.createCell(3).setCellValue(row.getStudentCount() != null ? row.getStudentCount() : 0);
                dataRow.createCell(4).setCellValue(row.getStatus() != null ? row.getStatus() : "");
                dataRow.createCell(5).setCellValue(row.getUpdatedAt() != null ? row.getUpdatedAt() : "");
            }

            workbook.write(response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            log.error("课程导出失败", e);
            throw new BusinessException(ErrorCode.COURSE_EXCEL_GENERATE_FAILED);
        }
    }

    // ==================== 修改课程状态 ====================

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public CourseStatusVO updateCourseStatus(Long courseId, CourseStatusDTO dto) {
        // 1. 校验课程存在
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        Integer currentStatus = course.getStatus() != null ? course.getStatus() : 0;
        String targetStatus = dto.getStatus();

        // 2. 校验目标状态合法性
        int targetCode = switch (targetStatus) {
            case "DRAFT" -> 0;
            case "PUBLISHED" -> 1;
            case "OFFLINE" -> 2;
            default -> throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID, "状态仅支持 DRAFT、PUBLISHED、OFFLINE");
        };

        // 3. 重复状态检查
        if (currentStatus == targetCode) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID, "课程已处于目标状态");
        }

        // 4. 校验状态流转合法性
        validateStatusTransition(currentStatus, targetCode);

        // 5. 发布/重新发布时执行完整校验
        if (targetCode == 1) {
            validatePublishable(courseId);
        }

        // 6. 更新状态
        LocalDateTime now = LocalDateTime.now();
        course.setStatus(targetCode);
        course.setUpdatedAt(now);

        // 发布时写入发布时间
        if (targetCode == 1) {
            course.setPublishedAt(now);
        }

        courseMapper.updateById(course);

        // 课程状态变更，清除课程结构缓存
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.DEPARTMENT_VISIBLE_COURSES));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.LEARNER_HOME));

        // 7. 构造响应
        CourseStatusVO vo = new CourseStatusVO();
        vo.setCourseId(course.getId());
        vo.setStatus(targetStatus);
        vo.setPublishedAt(course.getPublishedAt());
        vo.setUpdatedAt(now);
        return vo;
    }

    // ==================== 删除课程 ====================

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        // 仅草稿状态可删除
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_NOT_DRAFT);
        }

        // 1. 查询所有章节
        List<CourseChapter> chapters = courseChapterMapper.selectList(
                Wrappers.<CourseChapter>lambdaQuery().eq(CourseChapter::getCourseId, courseId));
        List<Long> chapterIds = chapters.stream().map(CourseChapter::getId).toList();

        // 2. 查询所有课程点
        if (!chapterIds.isEmpty()) {
            List<CoursePoint> points = coursePointMapper.selectList(
                    Wrappers.<CoursePoint>lambdaQuery().in(CoursePoint::getChapterId, chapterIds));
            List<Long> pointIds = points.stream().map(CoursePoint::getId).toList();

            // 3. 删除课件关联关系（硬删除，关系表无 @TableLogic）
            if (!pointIds.isEmpty()) {
                coursePointArticleMapper.delete(
                        Wrappers.<CoursePointArticle>lambdaQuery().in(CoursePointArticle::getCoursePointId, pointIds));
                coursePointVideoMapper.delete(
                        Wrappers.<CoursePointVideo>lambdaQuery().in(CoursePointVideo::getCoursePointId, pointIds));
                coursePointPptMapper.delete(
                        Wrappers.<CoursePointPpt>lambdaQuery().in(CoursePointPpt::getCoursePointId, pointIds));
            }

            // 4. 软删除课程点
            for (CoursePoint point : points) {
                coursePointMapper.deleteById(point.getId());
            }
        }

        // 5. 软删除章节
        for (CourseChapter chapter : chapters) {
            courseChapterMapper.deleteById(chapter.getId());
        }

        // 6. 删除课程关联数据
        courseTagMapper.delete(
                Wrappers.<CourseTag>lambdaQuery().eq(CourseTag::getCourseId, courseId));
        courseDepartmentMapper.delete(
                Wrappers.<CourseDepartment>lambdaQuery().eq(CourseDepartment::getCourseId, courseId));

        // 7. 软删除课程
        courseMapper.deleteById(courseId);

        // 课程删除，清除课程结构缓存
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.DEPARTMENT_VISIBLE_COURSES));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.LEARNER_HOME));
    }

    /**
     * 校验状态流转是否合法
     */
    private void validateStatusTransition(int current, int target) {
        boolean valid = switch (current) {
            case 0 -> target == 1;           // DRAFT -> PUBLISHED
            case 1 -> target == 0 || target == 2; // PUBLISHED -> DRAFT / OFFLINE
            case 2 -> target == 0 || target == 1; // OFFLINE -> DRAFT / PUBLISHED
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID,
                    "不允许从 " + statusName(current) + " 流转到 " + statusName(target));
        }
    }

    private String statusName(int code) {
        return switch (code) {
            case 0 -> "DRAFT";
            case 1 -> "PUBLISHED";
            case 2 -> "OFFLINE";
            default -> "UNKNOWN";
        };
    }

    /**
     * 发布校验：基础信息、标签、部门、章节、必修课程点和有效课件规则
     */
    private void validatePublishable(Long courseId) {
        // 1. 校验基础信息（标题、讲师、封面）
        Course course = courseMapper.selectById(courseId);
        if (course.getTitle() == null || course.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.COURSE_STRUCTURE_NOT_PUBLISHABLE, "课程标题不能为空");
        }
        if (course.getInstructorId() == null) {
            throw new BusinessException(ErrorCode.COURSE_STRUCTURE_NOT_PUBLISHABLE, "课程未设置讲师");
        }

        // 2. 校验标签（最多 3 个）
        List<CourseTag> tags = courseTagMapper.selectList(
                Wrappers.<CourseTag>lambdaQuery().eq(CourseTag::getCourseId, courseId));
        if (tags.isEmpty()) {
            throw new BusinessException(ErrorCode.COURSE_STRUCTURE_NOT_PUBLISHABLE, "课程至少需要选择一个标签");
        }

        // 3. 校验部门
        List<CourseDepartment> depts = courseDepartmentMapper.selectList(
                Wrappers.<CourseDepartment>lambdaQuery().eq(CourseDepartment::getCourseId, courseId));
        if (depts.isEmpty()) {
            throw new BusinessException(ErrorCode.COURSE_STRUCTURE_NOT_PUBLISHABLE, "课程至少需要配置一个发布部门");
        }

        // 4. 校验章节
        Long chapterCount = courseChapterMapper.selectCount(
                Wrappers.<CourseChapter>lambdaQuery()
                        .eq(CourseChapter::getCourseId, courseId)
                        .eq(CourseChapter::getStatus, 1));
        if (chapterCount == 0) {
            throw new BusinessException(ErrorCode.COURSE_STRUCTURE_NOT_PUBLISHABLE, "课程至少需要包含一个启用章节");
        }

        // 5. 校验课程点及课件
        List<CourseChapter> chapters = courseChapterMapper.selectList(
                Wrappers.<CourseChapter>lambdaQuery()
                        .eq(CourseChapter::getCourseId, courseId)
                        .eq(CourseChapter::getStatus, 1));

        boolean hasValidPoint = false;
        for (CourseChapter chapter : chapters) {
            List<CoursePoint> points = coursePointMapper.selectList(
                    Wrappers.<CoursePoint>lambdaQuery()
                            .eq(CoursePoint::getChapterId, chapter.getId())
                            .eq(CoursePoint::getStatus, 1));
            for (CoursePoint point : points) {
                hasValidPoint = true;
                // 校验每个课程点至少关联一个已发布课件
                long mediaCount = countPublishedMedia(point.getId());
                if (mediaCount == 0) {
                    throw new BusinessException(ErrorCode.COURSE_STRUCTURE_NOT_PUBLISHABLE,
                            "课程点「" + point.getTitle() + "」未关联任何有效课件");
                }
            }
        }
        if (!hasValidPoint) {
            throw new BusinessException(ErrorCode.COURSE_STRUCTURE_NOT_PUBLISHABLE, "课程至少需要包含一个启用课程点");
        }
    }

    private long countPublishedMedia(Long pointId) {
        long count = 0;
        List<CoursePointArticle> cpas = coursePointArticleMapper.selectList(
                Wrappers.<CoursePointArticle>lambdaQuery().eq(CoursePointArticle::getCoursePointId, pointId));
        for (CoursePointArticle cpa : cpas) {
            Article a = articleMapper.selectById(cpa.getArticleId());
            if (a != null && Integer.valueOf(1).equals(a.getStatus())) count++;
        }
        List<CoursePointVideo> cpvs = coursePointVideoMapper.selectList(
                Wrappers.<CoursePointVideo>lambdaQuery().eq(CoursePointVideo::getCoursePointId, pointId));
        for (CoursePointVideo cpv : cpvs) {
            Video v = videoMapper.selectById(cpv.getVideoId());
            if (v != null && Integer.valueOf(1).equals(v.getStatus())) count++;
        }
        List<CoursePointPpt> cpps = coursePointPptMapper.selectList(
                Wrappers.<CoursePointPpt>lambdaQuery().eq(CoursePointPpt::getCoursePointId, pointId));
        for (CoursePointPpt cpp : cpps) {
            Ppt p = pptMapper.selectById(cpp.getPptId());
            if (p != null && Integer.valueOf(1).equals(p.getStatus())) count++;
        }
        return count;
    }

    /**
     * 构建单个统计项，计算变化率和方向
     */
    private StatItemVO buildStatItem(long currentValue, Long previousValue) {
        StatItemVO item = new StatItemVO();
        item.setValue(currentValue);
        item.setPreviousValue(previousValue);

        if (previousValue == null) {
            item.setChangeRate(null);
            item.setChangeDirection("NO_DATA");
        } else if (previousValue == 0) {
            // 上月为 0，当前有值则视为 100% 增长
            if (currentValue == 0) {
                item.setChangeRate(BigDecimal.ZERO);
                item.setChangeDirection("SAME");
            } else {
                item.setChangeRate(null);
                item.setChangeDirection("UP");
            }
        } else {
            BigDecimal rate = BigDecimal.valueOf(currentValue - previousValue)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(previousValue), 1, RoundingMode.HALF_UP);
            item.setChangeRate(rate);

            if (rate.compareTo(BigDecimal.ZERO) > 0) {
                item.setChangeDirection("UP");
            } else if (rate.compareTo(BigDecimal.ZERO) < 0) {
                item.setChangeDirection("DOWN");
            } else {
                item.setChangeDirection("SAME");
            }
        }

        return item;
    }
}
