package org.example.nursingtrainingbackend.modules.courseware.article.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleMapper;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleStatSnapshotMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 文章统计快照定时任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSnapshotService {

    private final ArticleMapper articleMapper;
    private final ArticleStatSnapshotMapper snapshotMapper;

    /**
     * 每天凌晨00:05执行，写入前一天的统计快照
     * cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void createDailySnapshot() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成文章统计快照，日期：{}", yesterday);

        try {
            // 统计前一天的数据
            long totalArticles = articleMapper.countTotalArticles();
            long publishedArticles = articleMapper.countPublishedArticles();
            long draftArticles = articleMapper.countDraftArticles();
            
            // 当前阶段不提供浏览量采集接口，monthlyViews 暂时设为 0
            long monthlyViews = 0L;

            // 写入快照（使用 ON DUPLICATE KEY UPDATE，同一天重复执行会覆盖）
            snapshotMapper.upsertSnapshot(yesterday, totalArticles, publishedArticles, draftArticles, monthlyViews);

            log.info("文章统计快照生成成功，日期：{}，总数：{}，已发布：{}，草稿：{}", 
                    yesterday, totalArticles, publishedArticles, draftArticles);
        } catch (Exception e) {
            log.error("生成文章统计快照失败，日期：{}", yesterday, e);
            throw e;
        }
    }
}
