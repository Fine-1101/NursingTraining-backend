package org.example.nursingtrainingbackend.modules.course.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.annotation.DistributedLock;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.entity.CourseStatSnapshot;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseStatSnapshotMapper;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleStatSnapshotMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptStatSnapShotMapper;
import org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoStatSnapshotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatSnapshotJob {
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseStatSnapshotMapper courseStatSnapshotMapper;
    @Autowired
    private ArticleStatSnapshotMapper articleStatSnapshotMapper;
    @Autowired
    private VideoStatSnapshotMapper videoStatSnapshotMapper;
    @Autowired
    private PptStatSnapShotMapper pptStatSnapshotMapper;

    @Scheduled(cron = "0 10 0 * * ?")
    @DistributedLock(key = "job:daily_snapshot", leaseTime = 300, waitTime = 60)
    @Transactional(rollbackFor = Exception.class)
    public void generateDailySnapshot() {
        LocalDate today = LocalDate.now();
        log.info("开始生成统计快照, statDate={}", today);

        try {
            generateCourseSnapshot(today);
            generateArticleSnapshot(today);
            generateVideoSnapshot(today);
            generatePptSnapshot(today);
            log.info("统计快照生成完成, statDate={}", today);
        } catch (Exception e) {
            log.error("统计快照生成失败, statDate={}", today, e);
            throw e;
        }
    }


    private void generateCourseSnapshot(LocalDate statDate){
    long total=courseMapper.selectCount(buildCourseQuery(null));
    long draft=courseMapper.selectCount(buildCourseQuery(0));
    long published=courseMapper.selectCount(buildCourseQuery(1));
    long offline=courseMapper.selectCount(buildCourseQuery(2));

        CourseStatSnapshot snapshot = new CourseStatSnapshot();
        snapshot.setStatDate(statDate);
        snapshot.setTotalCourses(total);
        snapshot.setDraftCourses(draft);
        snapshot.setPublishedCourses(published);
        snapshot.setOfflineCourses(offline);

        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setUpdatedAt(LocalDateTime.now());

        courseStatSnapshotMapper.insert(snapshot);

    }

    private void generateArticleSnapshot(LocalDate statDate) {
        LocalDateTime monthStart = statDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextDay = statDate.plusDays(1).atStartOfDay();

        long total = countArticles(null, nextDay);
        long published = countArticles(1, nextDay);
        long draft = countArticles(0, nextDay);
        long monthlyViews = countArticleMonthlyViews(monthStart, nextDay);

        articleStatSnapshotMapper.upsertSnapshot(statDate, total, published, draft, monthlyViews);
        log.info("文章快照: total={}, published={}, draft={}, monthlyViews={}", total, published, draft, monthlyViews);
    }

    private void generateVideoSnapshot(LocalDate statDate) {
        LocalDateTime nextDay = statDate.plusDays(1).atStartOfDay();

        long total = countVideos(null, nextDay);
        long published = countVideos(1, nextDay);
        long draft = countVideos(0, nextDay);
        long storageBytes = sumVideoStorage(nextDay);

        videoStatSnapshotMapper.upsertSnapshot(statDate, total, storageBytes, published, draft);
        log.info("视频快照: total={}, published={}, draft={}, storageBytes={}", total, published, draft, storageBytes);
    }

    private void generatePptSnapshot(LocalDate statDate) {
        LocalDateTime monthStart = statDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextDay = statDate.plusDays(1).atStartOfDay();

        long total = countPpts(null, nextDay);
        long published = countPpts(1, nextDay);
        long draft = countPpts(0, nextDay);
        long monthlyAdded = countPptsAddedAfter(monthStart, nextDay);

        pptStatSnapshotMapper.upsertSnapshot(statDate, total, published, draft, monthlyAdded);
        log.info("PPT快照: total={}, published={}, draft={}, monthlyAdded={}", total, published, draft, monthlyAdded);
    }


    // ========== 课程查询辅助 ==========

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Course> buildCourseQuery(Integer status) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Course> qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(Course::getStatus, status);
        }
        return qw;
    }

    // ========== 文章查询辅助 ==========

    private long countArticles(Integer status, LocalDateTime beforeTime) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.nursingtrainingbackend.modules.courseware.article.entity.Article> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(org.example.nursingtrainingbackend.modules.courseware.article.entity.Article::getStatus, status);
        }
        qw.lt(org.example.nursingtrainingbackend.modules.courseware.article.entity.Article::getCreatedAt, beforeTime);
        return new com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleMapper, org.example.nursingtrainingbackend.modules.courseware.article.entity.Article>() {}.count(qw);
    }

    private long countArticleMonthlyViews(LocalDateTime monthStart, LocalDateTime nextDay) {
        // 简化处理：统计当月新增浏览量
        return 0L;
    }

    // ========== 视频查询辅助 ==========

    private long countVideos(Integer status, LocalDateTime beforeTime) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.nursingtrainingbackend.modules.courseware.video.entity.Video> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(org.example.nursingtrainingbackend.modules.courseware.video.entity.Video::getStatus, status);
        }
        qw.lt(org.example.nursingtrainingbackend.modules.courseware.video.entity.Video::getCreatedAt, beforeTime);
        return new com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoMapper, org.example.nursingtrainingbackend.modules.courseware.video.entity.Video>() {}.count(qw);
    }

    private long sumVideoStorage(LocalDateTime beforeTime) {
        return 0L;
    }

    // ========== PPT查询辅助 ==========

    private long countPpts(Integer status, LocalDateTime beforeTime) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt::getStatus, status);
        }
        qw.lt(org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt::getCreatedAt, beforeTime);
        return new com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptMapper, org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt>() {}.count(qw);
    }

    private long countPptsAddedAfter(LocalDateTime afterTime, LocalDateTime beforeTime) {
        return 0L;
    }
}
