package org.example.nursingtrainingbackend.modules.tag.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

import org.example.nursingtrainingbackend.modules.course.mapper.CourseTagMapper;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.tag.dto.TagBatchDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagCreateDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagQueryDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagStatusDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagUpdateDTO;
import org.example.nursingtrainingbackend.modules.tag.entity.CourseTag;
import org.example.nursingtrainingbackend.modules.tag.entity.Tag;
import org.example.nursingtrainingbackend.modules.tag.mapper.TagMapper;
import org.example.nursingtrainingbackend.modules.tag.mapper.TagStatisticsRow;
import org.example.nursingtrainingbackend.modules.tag.mapper.TagWithCount;
import org.example.nursingtrainingbackend.modules.tag.service.TagService;
import org.example.nursingtrainingbackend.modules.tag.vo.TagBatchResultVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagItemVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagOverviewVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagStatisticsItemVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagStatisticsVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagStatusVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TopTagVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private static final String OVERVIEW_CACHE_KEY = "nursing:tag:overview:v1";
    private static final Duration OVERVIEW_CACHE_TTL = Duration.ofMinutes(5);

    private static final String STATISTICS_CACHE_KEY = "nursing:tag:statistics:v1";
    private static final Duration STATISTICS_CACHE_TTL = Duration.ofMinutes(5);

    private final TagMapper tagMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CourseTagMapper courseTagMapper;

    @Override
    public PageResult<TagItemVO> queryPage(TagQueryDTO query) {
        IPage<TagWithCount> page = new Page<>(query.getPage(), query.getSize());
        IPage<TagWithCount> result = tagMapper.selectPageWithCount(
                page,
                query.getKeyword(),
                query.getStatus(),
                query.getSortBy(),
                query.getSortOrder()
        );
        List<TagItemVO> records = result.getRecords().stream()
                .map(this::toItemVO)
                .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public TagItemVO getDetail(Long id) {
        TagWithCount tag = tagMapper.selectDetailWithCount(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        return toItemVO(tag);
    }

    @Override
    @Transactional
    public TagItemVO createTag(TagCreateDTO dto) {
        Long exists = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery().eq(Tag::getName, dto.getName()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.TAG_NAME_EXISTS);
        }
        Tag tag = new Tag();
        tag.setName(dto.getName());
        tag.setColor(dto.getColor());
        tag.setStatus(dto.getStatus());
        tagMapper.insert(tag);
        clearOverviewCacheAfterCommit();
        return toItemVO(tagMapper.selectDetailWithCount(tag.getId()));
    }

    @Override
    @Transactional
    public TagItemVO updateTag(Long id, TagUpdateDTO dto) {
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        Long sameName = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getName, dto.getName())
                .ne(Tag::getId, id));
        if (sameName != null && sameName > 0) {
            throw new BusinessException(ErrorCode.TAG_NAME_EXISTS);
        }
        existing.setName(dto.getName());
        existing.setColor(dto.getColor());
        existing.setStatus(dto.getStatus());
        tagMapper.updateById(existing);
        clearOverviewCacheAfterCommit();
        return toItemVO(tagMapper.selectDetailWithCount(id));
    }

    @Override
    @Transactional
    public TagStatusVO updateStatus(Long id, TagStatusDTO dto) {
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        tagMapper.update(null, Wrappers.<Tag>lambdaUpdate()
                .eq(Tag::getId, id)
                .set(Tag::getStatus, dto.getStatus()));
        clearOverviewCacheAfterCommit();
        TagStatusVO vo = new TagStatusVO();
        vo.setId(id);
        vo.setStatus(dto.getStatus());
        return vo;
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        Long relatedCourses = courseTagMapper.selectCount(Wrappers.<CourseTag>lambdaQuery()
                .eq(CourseTag::getTagId, id));
        if (relatedCourses != null && relatedCourses > 0) {
            throw new BusinessException(ErrorCode.TAG_HAS_COURSES);
        }
        tagMapper.deleteById(id);
        clearOverviewCacheAfterCommit();
    }

    @Override
    @Transactional
    public TagBatchResultVO batchOperate(TagBatchDTO dto) {
        TagBatchResultVO result = new TagBatchResultVO();
        result.setRequestedCount(dto.getIds().size());

        int affected = 0;
        String action = dto.getAction();

        if ("ENABLE".equals(action) || "DISABLE".equals(action)) {
            int newStatus = "ENABLE".equals(action) ? 1 : 0;
            for (Long id : dto.getIds()) {
                int rows = tagMapper.update(null, Wrappers.<Tag>lambdaUpdate()
                        .eq(Tag::getId, id)
                        .set(Tag::getStatus, newStatus));
                affected += rows;
            }
        } else if ("DELETE".equals(action)) {
            for (Long id : dto.getIds()) {
                Tag tag = tagMapper.selectById(id);
                if (tag == null) {
                    continue;
                }
                Long relatedCourses = courseTagMapper.selectCount(Wrappers.<CourseTag>lambdaQuery()
                        .eq(CourseTag::getTagId, id));
                if (relatedCourses != null && relatedCourses > 0) {
                    throw new BusinessException(ErrorCode.TAG_HAS_COURSES,
                            "标签(id=" + id + ")已关联课程，无法批量删除");
                }
            }
            for (Long id : dto.getIds()) {
                affected += tagMapper.deleteById(id);
            }
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的批量操作: " + action);
        }

        result.setAffectedCount(affected);
        clearOverviewCacheAfterCommit();
        return result;
    }

    @Override
    public TagOverviewVO getOverview() {
        try {
            String cached = redisTemplate.opsForValue().get(OVERVIEW_CACHE_KEY);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, TagOverviewVO.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read tag overview cache", e);
        }

        TagOverviewVO overview = buildOverviewFromDB();

        try {
            redisTemplate.opsForValue().set(OVERVIEW_CACHE_KEY,
                    objectMapper.writeValueAsString(overview), OVERVIEW_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache tag overview", e);
        }
        return overview;
    }

    private TagOverviewVO buildOverviewFromDB() {
        TagOverviewVO overview = new TagOverviewVO();

        Long totalTags = tagMapper.selectCount(null);
        Long enabledTags = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery().eq(Tag::getStatus, 1));
        Long disabledTags = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery().eq(Tag::getStatus, 0));
        Long usedTags = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                .inSql(Tag::getId, "SELECT DISTINCT tag_id FROM course_tag"));

        overview.setTotalTags(nullSafe(totalTags));
        overview.setEnabledTags(nullSafe(enabledTags));
        overview.setDisabledTags(nullSafe(disabledTags));
        overview.setUsedTags(nullSafe(usedTags));
        overview.setUnusedTags(Math.max(0, nullSafe(totalTags) - nullSafe(usedTags)));

        List<TopTagVO> topTags = tagMapper.selectTopTags(5);
        for (int i = 0; i < topTags.size(); i++) {
            topTags.get(i).setRank(i + 1);
        }
        overview.setTopTags(topTags);
        return overview;
    }

    private TagItemVO toItemVO(TagWithCount tag) {
        TagItemVO vo = new TagItemVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setColor(tag.getColor());
        vo.setStatus(tag.getStatus());
        vo.setCourseCount(nullSafe(tag.getCourseCount()));
        vo.setCreatedAt(tag.getCreatedAt());
        vo.setUpdatedAt(tag.getUpdatedAt());
        return vo;
    }

    @Override
    public TagStatisticsVO getStatistics() {
        try {
            String cached = redisTemplate.opsForValue().get(STATISTICS_CACHE_KEY);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, TagStatisticsVO.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read tag statistics cache", e);
        }

        TagStatisticsVO statistics = buildStatisticsFromDB();

        try {
            redisTemplate.opsForValue().set(STATISTICS_CACHE_KEY,
                    objectMapper.writeValueAsString(statistics), STATISTICS_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache tag statistics", e);
        }
        return statistics;
    }

    private TagStatisticsVO buildStatisticsFromDB() {
        List<TagStatisticsRow> rows = tagMapper.selectTagStatistics();
        TagStatisticsVO vo = new TagStatisticsVO();
        Long totalAssociations = rows.isEmpty() ? 0L : rows.get(0).getTotalAssociations();
        vo.setTotalAssociations(totalAssociations == null ? 0L : totalAssociations);
        vo.setTotalTags(rows.size());
        vo.setGeneratedAt(Instant.now());

        List<TagStatisticsItemVO> items = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            TagStatisticsRow row = rows.get(i);
            TagStatisticsItemVO item = new TagStatisticsItemVO();
            item.setTagId(row.getId());
            item.setTagName(row.getName());
            item.setColor(row.getColor());
            item.setCourseCount(nullSafe(row.getCourseCount()));
            BigDecimal rate = row.getUsageRate() == null ? BigDecimal.ZERO
                    : row.getUsageRate().setScale(2, RoundingMode.HALF_UP);
            item.setUsageRate(rate);
            item.setUsageRateText(rate.toPlainString() + "%");
            item.setSortRank(i + 1);
            items.add(item);
        }
        vo.setItems(items);
        return vo;
    }

    private Long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private void clearOverviewCacheAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        redisTemplate.delete(OVERVIEW_CACHE_KEY);
                    } catch (Exception e) {
                        log.warn("Failed to clear tag overview cache after commit", e);
                    }
                    try {
                        redisTemplate.delete(STATISTICS_CACHE_KEY);
                    } catch (Exception e) {
                        log.warn("Failed to clear tag statistics cache after commit", e);
                    }
                }
            });
        } else {
            try {
                redisTemplate.delete(OVERVIEW_CACHE_KEY);
            } catch (Exception e) {
                log.warn("Failed to clear tag overview cache", e);
            }
            try {
                redisTemplate.delete(STATISTICS_CACHE_KEY);
            } catch (Exception e) {
                log.warn("Failed to clear tag statistics cache", e);
            }
        }
    }
}
