package org.example.nursingtrainingbackend.modules.courseware.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.nursingtrainingbackend.common.event.CacheEvictionEvent;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoBatchRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoStatusUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoUploadRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.Video;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.VideoStatSnapshot;
import org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoMapper;
import org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoStatSnapshotMapper;
import org.example.nursingtrainingbackend.modules.courseware.video.service.VideoService;
import org.example.nursingtrainingbackend.modules.courseware.video.vo.*;
import org.example.nursingtrainingbackend.modules.file.service.FileService;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import org.example.nursingtrainingbackend.config.OssConfig;
import org.example.nursingtrainingbackend.modules.course.entity.CoursePointVideo;
import org.example.nursingtrainingbackend.modules.course.mapper.CoursePointVideoMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.net.URL;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VideoServiceImpl implements VideoService {

    private static final String VIDEO_PLAY_URL_CACHE_PREFIX = "nursing:video:play-url:v1:";

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VideoStatSnapshotMapper snapshotMapper;

    @Autowired
    private CoursePointVideoMapper coursePointVideoMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private OSS ossClient;

    @Autowired(required = false)
    private OssConfig ossConfig;
    /** 生成指定视频的临时播放地址。 */

    @Override
    public VideoPlayUrlVO getVideoPlayUrl(Long id, Integer expiresIn) {

        if (expiresIn == null || expiresIn < 60) {
            expiresIn = 600;
        }
        if (expiresIn > 3600) {
            expiresIn = 3600;
        }

        String cacheKey = VIDEO_PLAY_URL_CACHE_PREFIX + id;
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                return objectMapper.readValue(cachedJson, new TypeReference<VideoPlayUrlVO>() {});
            }
        } catch (Exception e) {
            log.warn("读取视频播放URL缓存失败, videoId={}", id, e);
        }
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getId, id).isNull(Video::getDeletedAt);
        Video video = videoMapper.selectOne(wrapper);
        if (video == null) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }


        String key = extractOssKey(video.getVideoUrl());

        try {
            ossClient.getObjectMetadata(ossConfig.getBucketName(), key);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VIDEO_OSS_OBJECT_NOT_FOUND);
        }

        Date expiration = new Date(System.currentTimeMillis() + expiresIn * 1000L);
        URL signedUrl = ossClient.generatePresignedUrl(ossConfig.getBucketName(), key, expiration);

        VideoPlayUrlVO vo = new VideoPlayUrlVO();
        vo.setPlayUrl(signedUrl.toString());
        vo.setContentType("video/mp4");
        vo.setDuration(video.getDuration());
        vo.setExpireAt(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(expiration.getTime()),
                java.time.ZoneId.of("Asia/Shanghai")));

        long cacheTtl = expiresIn - 30;
        if (cacheTtl > 0) {
            try {
                String json = objectMapper.writeValueAsString(vo);
                redisTemplate.opsForValue().set(cacheKey, json, cacheTtl, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("写入视频播放URL缓存失败, videoId={}", id, e);
            }
        }
        return vo;
    }
    /** 分页或按条件查询视频。 */
    @Override
    public PageResult<VideoListItemVO> listVideos(String keyword, String status,
                                                  String uploadedFrom, String uploadedTo,
                                                  String sortOrder, Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }
        if (size > 100) {
            size = 100;
        }

        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Video::getDeletedAt);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Video::getTitle, keyword)
                    .or().like(Video::getDescription, keyword)
                    .or().like(Video::getOriginalName, keyword));

            List<Long> matchedUserIds = findUserIdsByKeyword(keyword);
            if (!matchedUserIds.isEmpty()) {
                wrapper.or(w -> w.in(Video::getCreatedBy, matchedUserIds));
            }
        }

        if (StringUtils.hasText(status)) {
            Integer statusCode = convertStatusToCode(status);
            if (statusCode != null) {
                wrapper.eq(Video::getStatus, statusCode);
            }
        }

        if (StringUtils.hasText(uploadedFrom)) {
            LocalDateTime fromDateTime = LocalDate.parse(uploadedFrom).atStartOfDay();
            wrapper.ge(Video::getCreatedAt, fromDateTime);
        }
        if (StringUtils.hasText(uploadedTo)) {
            LocalDateTime toDateTime = LocalDate.parse(uploadedTo).atTime(LocalTime.MAX);
            wrapper.le(Video::getCreatedAt, toDateTime);
        }

        if ("asc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(Video::getCreatedAt).orderByAsc(Video::getId);
        } else {
            wrapper.orderByDesc(Video::getCreatedAt).orderByDesc(Video::getId);
        }

        Page<Video> pageParam = new Page<>(page, size);
        Page<Video> resultPage = videoMapper.selectPage(pageParam, wrapper);

        List<VideoListItemVO> voList = resultPage.getRecords().stream()
                .map(this::convertToListItemVO)
                .toList();

        return new PageResult<>(voList, resultPage.getTotal(),
                resultPage.getCurrent(), resultPage.getSize(), resultPage.getPages());
    }

    private VideoListItemVO convertToListItemVO(Video video) {
        VideoListItemVO vo = new VideoListItemVO();
        vo.setId(video.getId());
        vo.setTitle(video.getTitle());
        vo.setDescription(video.getDescription());
        vo.setCoverUrl(video.getCoverUrl());
        vo.setDuration(video.getDuration());
        vo.setDurationText(formatDuration(video.getDuration()));
        vo.setFileSize(video.getFileSize());
        vo.setFileSizeText(formatFileSize(video.getFileSize()));
        vo.setUploaderId(video.getCreatedBy());
        vo.setUploaderName(getUploaderName(video.getCreatedBy()));
        vo.setUploadedAt(video.getCreatedAt());
        vo.setStatus(convertCodeToStatus(video.getStatus()));
        return vo;
    }

    private List<Long> findUserIdsByKeyword(String keyword) {
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.like(User::getRealName, keyword).select(User::getId);
        List<User> users = userMapper.selectList(userWrapper);
        return users.stream().map(User::getId).toList();
    }

    private String formatDuration(Integer totalSeconds) {
        if (totalSeconds == null || totalSeconds <= 0) {
            return "00:00";
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0 B";
        }
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        if (unitIndex == 0) {
            return String.format("%d B", bytes);
        }
        return String.format("%.1f %s", value, units[unitIndex]);
    }
    /** 获取视频概览。 */

    @Override
    public VideoOverviewVO getVideoOverview() {
        VideoOverviewVO overview = new VideoOverviewVO();

        long totalVideos = videoMapper.countTotalVideos();
        long publishedVideos = videoMapper.countPublishedVideos();
        long draftVideos = videoMapper.countDraftVideos();
        long storageBytes = videoMapper.sumFileSizeTotal();

        overview.setTotalVideos(totalVideos);
        overview.setPublishedVideos(publishedVideos);
        overview.setDraftVideos(draftVideos);
        overview.setStorageBytes(storageBytes);
        overview.setStorageText(formatFileSize(storageBytes));

        LocalDate lastMonthSameDay = LocalDate.now().minusMonths(1);
        if (lastMonthSameDay.getMonthValue() != LocalDate.now().getMonthValue() - 1 &&
                lastMonthSameDay.getMonthValue() != 12) {
            lastMonthSameDay = lastMonthSameDay.withDayOfMonth(lastMonthSameDay.lengthOfMonth());
        }

        VideoStatSnapshot lastMonthSnapshot = snapshotMapper.selectByDate(lastMonthSameDay);

        if (lastMonthSnapshot != null) {
            VideoOverviewVO.MonthOverMonth mom = new VideoOverviewVO.MonthOverMonth();
            mom.setTotalVideosRate(calculateRate(totalVideos, lastMonthSnapshot.getTotalVideos()));
            mom.setStorageRate(calculateRate(storageBytes, lastMonthSnapshot.getStorageBytes()));
            mom.setPublishedVideosRate(calculateRate(publishedVideos, lastMonthSnapshot.getPublishedVideos()));
            mom.setDraftVideosRate(calculateRate(draftVideos, lastMonthSnapshot.getDraftVideos()));
            overview.setMonthOverMonth(mom);
        } else {
            overview.setMonthOverMonth(null);
        }

        return overview;
    }
    /** 获取视频详情。 */

    @Override
    public VideoDetailVO getVideoDetail(Long id) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getId, id).isNull(Video::getDeletedAt);
        Video video = videoMapper.selectOne(wrapper);
        if (video == null) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }

        return convertToDetailVO(video);
    }
    /** 上传并登记视频。 */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoUploadResponseVO uploadVideo(VideoUploadRequest request, AuthenticatedUser user) {
        String title = request.getTitle().trim();
        if (title.isEmpty()) {
            throw new BusinessException(ErrorCode.VIDEO_OSS_INVALID, "视频标题不能为空");
        }

        if (!request.getOriginalName().toLowerCase().endsWith(".mp4")) {
            throw new BusinessException(ErrorCode.VIDEO_MP4_ONLY);
        }

        String videoKey = extractOssKey(request.getVideoUrl());
        validateVideoOssObject(videoKey, request.getFileSize());

        if (StringUtils.hasText(request.getCoverUrl())) {
            String coverKey = extractOssKey(request.getCoverUrl());
            validateCoverOssObject(coverKey);
        }

        String statusStr = request.getStatus();
        if (!"DRAFT".equalsIgnoreCase(statusStr) && !"PUBLISHED".equalsIgnoreCase(statusStr)) {
            throw new BusinessException(ErrorCode.VIDEO_STATUS_CONFLICT, "上传状态仅支持 DRAFT 或 PUBLISHED");
        }

        LocalDateTime now = LocalDateTime.now();

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(request.getDescription());
        video.setVideoUrl(request.getVideoUrl());
        video.setOriginalName(request.getOriginalName());
        video.setCoverUrl(request.getCoverUrl());
        video.setDuration(request.getDuration());
        video.setFileSize(request.getFileSize());
        video.setAllowDrag(request.getAllowDrag() ? 1 : 0);
        video.setAllowSpeed(request.getAllowSpeed() ? 1 : 0);
        video.setAllowCache(request.getAllowCache() ? 1 : 0);
        video.setViewCount(0);
        video.setWatchCount(0);
        video.setCreatedBy(user.id());
        video.setUploadedAt(now);
        video.setCreatedAt(now);
        video.setUpdatedAt(now);

        if ("PUBLISHED".equalsIgnoreCase(statusStr)) {
            video.setStatus(1);
            video.setPublishedAt(now);
        } else {
            video.setStatus(0);
        }

        videoMapper.insert(video);
        
        // 标记视频文件和封面文件已使用
       // String videoKey = extractOssKey(request.getVideoUrl());
        fileService.markFileUsed(videoKey, "VIDEO_FILE", video.getId());
        
        if (request.getCoverUrl() != null && !request.getCoverUrl().isBlank()) {
            String coverKey = extractOssKey(request.getCoverUrl());
            fileService.markFileUsed(coverKey, "VIDEO_COVER", video.getId());
        }

        VideoUploadResponseVO response = new VideoUploadResponseVO();
        response.setId(video.getId());
        response.setTitle(video.getTitle());
        response.setUploaderId(user.id());
        response.setUploaderName(user.nickname());
        response.setStatus(convertCodeToStatus(video.getStatus()));
        response.setUploadedAt(video.getUploadedAt());
        response.setPublishedAt(video.getPublishedAt());

        return response;
    }

    private String extractOssKey(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException(ErrorCode.VIDEO_OSS_INVALID);
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                return path.startsWith("/") ? path.substring(1) : path;
            }
        } catch (java.net.URISyntaxException ignored) {
        }
        String trimmed = url.startsWith("/") ? url.substring(1) : url;
        String baseDir = ossConfig.getBaseDirectory();
        if (baseDir != null && trimmed.startsWith(baseDir + "/")) {
            return trimmed;
        }
        return baseDir != null ? baseDir + "/" + trimmed : trimmed;
    }

    private void validateVideoOssObject(String key, long expectedSize) {
//        if (!key.startsWith("nursing-training/videos/originals/")) {
//            throw new BusinessException(ErrorCode.VIDEO_OSS_INVALID, "视频对象必须位于 videos/originals/ 目录下");
//        }

        ObjectMetadata metadata;
        try {
            metadata = ossClient.getObjectMetadata(ossConfig.getBucketName(), key);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VIDEO_OSS_OBJECT_NOT_FOUND);
        }

        String contentType = metadata.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("video/mp4")) {
            throw new BusinessException(ErrorCode.VIDEO_MP4_ONLY);
        }

        long actualSize = metadata.getContentLength();
        if (actualSize <= 0) {
            throw new BusinessException(ErrorCode.VIDEO_OSS_INVALID, "视频文件大小异常");
        }
        if (actualSize != expectedSize) {
            throw new BusinessException(ErrorCode.VIDEO_OSS_INVALID,
                    "文件大小不一致：声明 " + expectedSize + " 字节，实际 " + actualSize + " 字节");
        }
        if (actualSize > 104857600L) {
            throw new BusinessException(ErrorCode.VIDEO_FILE_SIZE_EXCEEDED);
        }
    }

    private void validateCoverOssObject(String key) {
        if (!key.startsWith("nursing-training/videos/covers/")) {
            throw new BusinessException(ErrorCode.VIDEO_OSS_INVALID, "封面对象必须位于 videos/covers/ 目录下");
        }

        try {
            ossClient.getObjectMetadata(ossConfig.getBucketName(), key);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VIDEO_OSS_OBJECT_NOT_FOUND, "封面文件不存在");
        }
    }
    /** 更新视频状态。 */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoStatusUpdateResponseVO updateVideoStatus(Long id, VideoStatusUpdateRequest request) {

        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getId, id)
                .isNull(Video::getDeletedAt);

        Video video = videoMapper.selectOne(wrapper);
        if (video == null) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }

        String targetStatusStr = request.getStatus().toUpperCase();
        Integer targetStatusCode = convertStatusToCode(targetStatusStr);
        if (targetStatusCode == null) {
            throw new BusinessException(ErrorCode.VIDEO_STATUS_CONFLICT, "非法状态值");
        }

        if (video.getStatus().equals(targetStatusCode)) {
            throw new BusinessException(ErrorCode.VIDEO_STATUS_CONFLICT, "视频状态未发生变化");
        }

        validateStatusTransition(video.getStatus(), targetStatusCode);

        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<Video> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Video::getId, id)
                .set(Video::getStatus, targetStatusCode)
                .set(Video::getUpdatedAt, now);

        if (targetStatusCode == 1) {
            String key = extractOssKey(video.getVideoUrl());
            try {
                ossClient.getObjectMetadata(ossConfig.getBucketName(), key);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.VIDEO_OSS_OBJECT_NOT_FOUND);
            }
            updateWrapper.set(Video::getPublishedAt, now);
        } else if (targetStatusCode == 0) {
            updateWrapper.set(Video::getPublishedAt, null);
        }

        videoMapper.update(null, updateWrapper);

        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.VIDEO_PLAY_URL));
        VideoStatusUpdateResponseVO response = new VideoStatusUpdateResponseVO();
        response.setId(video.getId());
        response.setStatus(convertCodeToStatus(targetStatusCode));
        response.setUpdatedAt(now);

        if (targetStatusCode == 1) {
            response.setPublishedAt(now);
        } else if (targetStatusCode == 0) {
            response.setPublishedAt(null);
        } else {
            response.setPublishedAt(video.getPublishedAt());
        }
        return response;
    }
    /** 更新视频。 */


    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoUpdateResponseVO updateVideo(Long id, VideoUpdateRequest request) {

        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getId, id)
                .isNull(Video::getDeletedAt);

        Video video = videoMapper.selectOne(wrapper);
        if (video == null) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }

        String title = request.getTitle().trim();
        if (title.isEmpty()) {
            throw new BusinessException(ErrorCode.VIDEO_OSS_INVALID, "视频标题不能为空");
        }

        if (StringUtils.hasText(request.getCoverUrl())) {
            String coverKey = extractOssKey(request.getCoverUrl());
            validateCoverOssObject(coverKey);
        }

        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<Video> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Video::getId, id)
                .set(Video::getTitle, title)
                .set(Video::getDescription, request.getDescription())
                .set(Video::getCoverUrl, request.getCoverUrl())
                .set(Video::getAllowDrag, request.getAllowDrag() ? 1 : 0)
                .set(Video::getAllowSpeed, request.getAllowSpeed() ? 1 : 0)
                .set(Video::getAllowCache, request.getAllowCache() ? 1 : 0)
                .set(Video::getUpdatedAt, now);

        videoMapper.update(null, updateWrapper);
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.VIDEO_PLAY_URL));


        VideoUpdateResponseVO response = new VideoUpdateResponseVO();
        response.setId(video.getId());
        response.setTitle(title);
        response.setDescription(request.getDescription());
        response.setCoverUrl(request.getCoverUrl());
        response.setAllowDrag(request.getAllowDrag());
        response.setAllowSpeed(request.getAllowSpeed());
        response.setAllowCache(request.getAllowCache());
        response.setStatus(convertCodeToStatus(video.getStatus()));
        response.setUpdatedAt(now);

        return response;
    }
    /** 删除视频。 */


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVideo(Long id) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getId, id).isNull(Video::getDeletedAt);
        Video video = videoMapper.selectOne(wrapper);
        if (video == null) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }

        long refCount = coursePointVideoMapper.selectCount(
                Wrappers.<CoursePointVideo>lambdaQuery()
                        .eq(CoursePointVideo::getVideoId, id));
        if (refCount > 0) {
            throw new BusinessException(ErrorCode.VIDEO_IN_USE);
        }

        LambdaUpdateWrapper<Video> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Video::getId, id)
                .set(Video::getDeletedAt, LocalDateTime.now());
        videoMapper.update(null, updateWrapper);

        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.VIDEO_PLAY_URL));

        String videoUrl = video.getVideoUrl();
        String coverUrl = video.getCoverUrl();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /** 在当前事务成功提交后执行后续处理。 */
            @Override
            public void afterCommit() {
                cleanupVideoOssObjects(videoUrl, coverUrl);
            }
        });
    }

    private void cleanupVideoOssObjects(String videoUrl, String coverUrl) {
        if (StringUtils.hasText(videoUrl)) {
            try {
                String videoKey = extractOssKey(videoUrl);
                ossClient.deleteObject(ossConfig.getBucketName(), videoKey);
                log.info("已删除 OSS 视频对象: {}", videoKey);
            } catch (Exception e) {
                log.error("OSS 视频对象删除失败，待重试: {}", videoUrl, e);
            }
        }

        if (StringUtils.hasText(coverUrl)) {
            try {
                String coverKey = extractOssKey(coverUrl);
                long coverRefCount = videoMapper.selectCount(
                        Wrappers.<Video>lambdaQuery()
                                .eq(Video::getCoverUrl, coverUrl)
                                .isNull(Video::getDeletedAt));
                if (coverRefCount == 0) {
                    ossClient.deleteObject(ossConfig.getBucketName(), coverKey);
                    log.info("已删除 OSS 封面对象: {}", coverKey);
                } else {
                    log.info("封面仍被 {} 个视频引用，跳过清理: {}", coverRefCount, coverKey);
                }
            } catch (Exception e) {
                log.error("OSS 封面对象删除失败，待重试: {}", coverUrl, e);
            }
        }
    }
    /** 批量发布视频。 */


    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoBatchPublishResponseVO batchPublishVideos(VideoBatchRequest request) {

        List<Long> ids = request.getIds();
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException(
                    ErrorCode.VIDEO_OSS_INVALID,
                    "视频ID列表不能为空"
            );
        }

        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Video::getId, ids)
                .isNull(Video::getDeletedAt);

        List<Video> videos = videoMapper.selectList(wrapper);

        // 有不存在或已删除的视频
        if (videos.size() != ids.size()) {
            throw new BusinessException(
                    ErrorCode.VIDEO_BATCH_DELETE_FAILED,
                    "存在不存在或已删除的视频"
            );
        }

        // 状态校验：只允许 DRAFT(0) / OFFLINE(2) → PUBLISHED(1)
        for (Video video : videos) {
            if (video.getStatus() != 0 && video.getStatus() != 2) {
                throw new BusinessException(
                        ErrorCode.VIDEO_STATUS_CONFLICT,
                        "存在状态不允许发布的视频"
                );
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // ✅ 批量更新（性能 & 事务一致性更好）
        LambdaUpdateWrapper<Video> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Video::getId, ids)
                .set(Video::getStatus, 1)
                .set(Video::getPublishedAt, now)
                .set(Video::getUpdatedAt, now);

        int updatedRows = videoMapper.update(null, updateWrapper);

        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.VIDEO_PLAY_URL));

        VideoBatchPublishResponseVO response = new VideoBatchPublishResponseVO();
        response.setRequestedCount(ids.size());
        response.setPublishedCount(updatedRows);
        response.setPublishedAt(now);

        return response;
    }
    /** 批量删除视频。 */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoBatchDeleteResponseVO batchDeleteVideos(VideoBatchRequest request) {
        List<Long> ids = request.getIds();
        LocalDateTime deleteTime = LocalDateTime.now();

        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Video::getId, ids).isNull(Video::getDeletedAt);
        List<Video> videos = videoMapper.selectList(wrapper);

        if (videos.size() != ids.size()) {
            throw new BusinessException(ErrorCode.VIDEO_BATCH_DELETE_FAILED, "存在不存在或已删除的视频");
        }

        long refCount = coursePointVideoMapper.selectCount(
                Wrappers.<CoursePointVideo>lambdaQuery()
                        .in(CoursePointVideo::getVideoId, ids));
        if (refCount > 0) {
            throw new BusinessException(ErrorCode.VIDEO_IN_USE, "部分视频已被课程点关联，无法删除");
        }

        LambdaUpdateWrapper<Video> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Video::getId, ids)
                .isNull(Video::getDeletedAt)
                .set(Video::getDeletedAt, deleteTime);
        int updatedRows = videoMapper.update(null, updateWrapper);

        List<String> videoUrls = videos.stream()
                .map(Video::getVideoUrl)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        List<String> coverUrls = videos.stream()
                .map(Video::getCoverUrl)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /** 在当前事务成功提交后执行后续处理。 */
            @Override
            public void afterCommit() {
                for (String videoUrl : videoUrls) {
                    try {
                        String videoKey = extractOssKey(videoUrl);
                        ossClient.deleteObject(ossConfig.getBucketName(), videoKey);
                        log.info("已删除 OSS 视频对象: {}", videoKey);
                    } catch (Exception e) {
                        log.error("OSS 视频对象删除失败，待重试: {}", videoUrl, e);
                    }
                }
                for (String coverUrl : coverUrls) {
                    try {
                        String coverKey = extractOssKey(coverUrl);
                        long coverRefCount = videoMapper.selectCount(
                                Wrappers.<Video>lambdaQuery()
                                        .eq(Video::getCoverUrl, coverUrl)
                                        .isNull(Video::getDeletedAt));
                        if (coverRefCount == 0) {
                            ossClient.deleteObject(ossConfig.getBucketName(), coverKey);
                            log.info("已删除 OSS 封面对象: {}", coverKey);
                        } else {
                            log.info("封面仍被 {} 个视频引用，跳过清理: {}", coverRefCount, coverKey);
                        }
                    } catch (Exception e) {
                        log.error("OSS 封面对象删除失败，待重试: {}", coverUrl, e);
                    }
                }
            }
        });

        VideoBatchDeleteResponseVO response = new VideoBatchDeleteResponseVO();
        response.setRequestedCount(ids.size());
        response.setDeletedCount(updatedRows);

        return response;
    }

    private VideoDetailVO convertToDetailVO(Video video) {
        VideoDetailVO vo = new VideoDetailVO();
        vo.setId(video.getId());
        vo.setTitle(video.getTitle());
        vo.setDescription(video.getDescription());
        vo.setOriginalName(video.getOriginalName());
        vo.setCoverUrl(video.getCoverUrl());
        vo.setDuration(video.getDuration());
        vo.setFileSize(video.getFileSize());
        vo.setAllowDrag(video.getAllowDrag() == 1);
        vo.setAllowSpeed(video.getAllowSpeed() == 1);
        vo.setAllowCache(video.getAllowCache() == 1);
        vo.setViewCount((long) video.getViewCount());
        vo.setWatchCount((long) video.getWatchCount());
        vo.setUploaderId(video.getCreatedBy());
        vo.setUploaderName(getUploaderName(video.getCreatedBy()));
        vo.setStatus(convertCodeToStatus(video.getStatus()));
        vo.setUploadedAt(video.getCreatedAt());
        vo.setPublishedAt(video.getPublishedAt());
        vo.setUpdatedAt(video.getUpdatedAt());
        return vo;
    }

    private String getUploaderName(Long userId) {
        if (userId == null) {
            return "未知用户";
        }
        User user = userMapper.selectById(userId);
        return user != null ? user.getRealName() : "未知用户";
    }

    private Integer convertStatusToCode(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "DRAFT" -> 0;
            case "PUBLISHED" -> 1;
            case "OFFLINE" -> 2;
            default -> null;
        };
    }

    private String convertCodeToStatus(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> "DRAFT";
            case 1 -> "PUBLISHED";
            case 2 -> "OFFLINE";
            default -> null;
        };
    }

    private Double calculateRate(long currentValue, long previousValue) {
        if (previousValue == 0) {
            return null;
        }
        return ((double)(currentValue - previousValue) / previousValue) * 100.0;
    }

    private void validateStatusTransition(Integer currentStatus, Integer targetStatus) {
        boolean valid = false;
        if (currentStatus == 0 && targetStatus == 1) {
            valid = true;
        } else if (currentStatus == 1 && targetStatus == 0) {
            valid = true;
        } else if (currentStatus == 1 && targetStatus == 2) {
            valid = true;
        } else if (currentStatus == 2 && targetStatus == 1) {
            valid = true;
        }

        if (!valid) {
            throw new BusinessException(ErrorCode.VIDEO_STATUS_CONFLICT);
        }
    }
}
