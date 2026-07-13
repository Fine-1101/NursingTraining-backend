package org.example.nursingtrainingbackend.modules.courseware.ppt.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.config.properties.OssProperties;
import org.example.nursingtrainingbackend.modules.courseware.ppt.dto.CreatePptRequest;
import org.example.nursingtrainingbackend.modules.courseware.ppt.dto.UpdatePptRequest;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.service.PptService;
import org.example.nursingtrainingbackend.modules.courseware.ppt.vo.PptDetailVO;
import org.example.nursingtrainingbackend.modules.courseware.ppt.vo.PptListItem;
import org.example.nursingtrainingbackend.modules.courseware.ppt.vo.PptOverviewVO;
import org.example.nursingtrainingbackend.modules.file.service.FileService;
//import org.example.nursingtrainingbackend.modules.file.service.FileService;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PptServiceImpl implements PptService {

    private final PptMapper pptMapper;
    private final UserMapper userMapper;
    private final OSS ossClient;
    private final OssProperties ossProperties;
    @Qualifier("ossFileServiceImpl")
    private final FileService fileService;


    private static final Set<String> VALID_TRANSITIONS = new HashSet<>(Arrays.asList(
            "DRAFT->PUBLISHED", "DRAFT->OFFLINE",
            "PUBLISHED->DRAFT", "PUBLISHED->OFFLINE",
            "OFFLINE->PUBLISHED"
    ));

    @Override
    @Transactional
    public Ppt createPpt(CreatePptRequest request, Long userId, String username) {
        validateOssFile(request.getOriginalUrl());

        Ppt ppt = new Ppt();
        ppt.setTitle(request.getTitle());
        ppt.setDescription(request.getDescription());
        ppt.setOriginalUrl(request.getOriginalUrl());
        ppt.setOriginalName(request.getOriginalName());
        ppt.setFileSize(request.getFileSize());
        // ✅ 修正：allowDownload 是 Integer 类型，0 或 1
        ppt.setAllowDownload(request.getAllowDownload() != null && request.getAllowDownload() ? 1 : 0);
        ppt.setCreatedBy(userId);
        ppt.setUploadedAt(LocalDateTime.now());
        // ✅ 修正：status 是 Integer 类型
        ppt.setStatus(mapStatusToInt(request.getStatus()));

        if ("PUBLISHED".equals(request.getStatus())) {
            ppt.setPublishedAt(LocalDateTime.now());
        }

        pptMapper.insert(ppt);

        // 标记PPT文件已使用
        String pptKey = extractKeyFromUrl(ppt.getOriginalUrl());
        fileService.markFileUsed(pptKey, "PPT_FILE", ppt.getId());

        return ppt;
    }

    @Override
    public IPage<PptListItem> pagePpt(String keyword, String status, String uploadedFrom,
                                     String uploadedTo, String sortOrder, Integer page, Integer size) {
        LambdaQueryWrapper<Ppt> wrapper = Wrappers.<Ppt>lambdaQuery()
                .isNull(Ppt::getDeletedAt);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(Ppt::getTitle, keyword)
                    .or()
                    .like(Ppt::getOriginalName, keyword)
            );
        }

        if (status != null && !status.isBlank()) {
            wrapper.eq(Ppt::getStatus, mapStatusToInt(status));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (uploadedFrom != null && !uploadedFrom.isBlank()) {
            wrapper.ge(Ppt::getUploadedAt, LocalDateTime.parse(uploadedFrom + " 00:00:00", formatter));
        }
        if (uploadedTo != null && !uploadedTo.isBlank()) {
            wrapper.le(Ppt::getUploadedAt, LocalDateTime.parse(uploadedTo + " 23:59:59", formatter));
        }

        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        wrapper.orderBy(true, isAsc, Ppt::getUploadedAt);
        wrapper.orderBy(true, isAsc, Ppt::getCreatedAt);

        IPage<Ppt> pptPage = pptMapper.selectPage(new Page<>(page, size), wrapper);
        return pptPage.convert(this::toListItem);
    }

    @Override
    public PptDetailVO getPptDetail(Long id) {
        Ppt ppt = getPptById(id);
        return toDetailVO(ppt);
    }

    @Override
    @Transactional
    public Ppt updatePpt(Long id, UpdatePptRequest request) {
        Ppt ppt = getPptById(id);
        ppt.setTitle(request.getTitle());
        if (request.getDescription() != null) {
            ppt.setDescription(request.getDescription());
        }
        // ✅ 修正：allowDownload 是 Integer 类型
        ppt.setAllowDownload(request.getAllowDownload() != null && request.getAllowDownload() ? 1 : 0);
        pptMapper.updateById(ppt);
        return ppt;
    }

    @Override
    @Transactional
    public Ppt updateStatus(Long id, String status) {
        Ppt ppt = getPptById(id);
        int from = ppt.getStatus() != null ? ppt.getStatus() : 0;
        int to = mapStatusToInt(status);

        if (from != to) {
            String fromStr = mapStatusToString(from);
            String toStr = mapStatusToString(to);
            String transition = fromStr + "->" + toStr;
            if (!VALID_TRANSITIONS.contains(transition)) {
                throw new BusinessException(ErrorCode.PPT_STATUS_INVALID,"不允许从 " + fromStr + " 流转到 " + toStr);
            }
        }

        // ✅ 修正：status 是 Integer 类型
        ppt.setStatus(to);
        if (to == 1) { // PUBLISHED
            ppt.setPublishedAt(LocalDateTime.now());
        } else if (to == 0) { // DRAFT
            ppt.setPublishedAt(null);
        }

        pptMapper.updateById(ppt);
        return ppt;
    }

    @Override
    public String getDownloadUrl(Long id, Integer expiresIn) {
        if (ossClient == null || ossProperties == null) {
            throw new BusinessException(ErrorCode.OSS_NOT_CONFIGURED);
        }
        Ppt ppt = getPptById(id);
        String key = extractKeyFromUrl(ppt.getOriginalUrl());

        if (expiresIn == null || expiresIn < 60) expiresIn = 600;
        if (expiresIn > 3600) expiresIn = 3600;

        // ✅ 添加 import java.util.Date
        Date expiration = new Date(System.currentTimeMillis() + expiresIn * 1000L);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                ossProperties.getBucketName(), key);
        request.setExpiration(expiration);

        URL url = ossClient.generatePresignedUrl(request);
        return url.toString();
    }

    @Override
    @Transactional
    public void deletePpt(Long id) {
        Ppt ppt = getPptById(id);
        // 使用 MyBatis Plus 的 deleteById，配合 @TableLogic 自动执行:
        // UPDATE ppt SET deleted_at = NOW() WHERE id = ? AND deleted_at IS NULL
        pptMapper.deleteById(id);
        asyncDeleteOssFile(ppt.getOriginalUrl());
    }

    // ==================== 私有方法 ====================

    @Override
    public PptOverviewVO getOverview() {
        long totalPpts = pptMapper.countTotalPpts();
        long publishedPpts = pptMapper.countPublishedPpts();
        long draftPpts = pptMapper.countDraftPpts();
        long monthlyAdded = pptMapper.countMonthlyAdded();

        return PptOverviewVO.builder()
                .totalPpts(totalPpts)
                .publishedPpts(publishedPpts)
                .draftPpts(draftPpts)
                .monthlyAdded(monthlyAdded)
                .monthOverMonth(null)
                .build();
    }

    private Ppt getPptById(Long id) {
        Ppt ppt = pptMapper.selectById(id);
        if (ppt == null || ppt.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.PPT_NOT_FOUND);
        }
        return ppt;
    }

    // ✅ 状态映射：String -> Integer
    private int mapStatusToInt(String status) {
        if (status == null) return 0;
        return switch (status.toUpperCase()) {
            case "DRAFT" -> 0;
            case "PUBLISHED" -> 1;
            case "OFFLINE" -> 2;
            default -> throw new BusinessException(ErrorCode.PPT_STATUS_INVALID,"无效状态: " + status);
        };
    }

    // ✅ 状态映射：Integer -> String
    private String mapStatusToString(int status) {
        return switch (status) {
            case 0 -> "DRAFT";
            case 1 -> "PUBLISHED";
            case 2 -> "OFFLINE";
            default -> "DRAFT";
        };
    }

    private void validateOssFile(String url) {
        /*try {
            String key = extractKeyFromUrl(url);
            ossClient.getObjectMetadata(ossProperties.getBucketName(), key);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OSS_FILE_NOT_FOUND);
        }

         */
    }

    private String extractKeyFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OSS_INVALID_URL);
        }
    }

    private PptListItem toListItem(Ppt ppt) {
        return PptListItem.builder()
                .id(ppt.getId())
                .title(ppt.getTitle())
                .originalName(ppt.getOriginalName())
                .fileSize(ppt.getFileSize())
                .fileSizeText(formatFileSize(ppt.getFileSize()))
                .courseCount(0L)
                .uploaderId(ppt.getCreatedBy())
                .uploaderName(getUploaderName(ppt.getCreatedBy()))
                .uploadedAt(ppt.getUploadedAt())
                .status(mapStatusToString(ppt.getStatus()))
                .build();
    }

    private PptDetailVO toDetailVO(Ppt ppt) {
        return PptDetailVO.builder()
                .id(ppt.getId())
                .title(ppt.getTitle())
                .description(ppt.getDescription())
                .originalName(ppt.getOriginalName())
                .fileSize(ppt.getFileSize())
                .courseCount(0L)
                .allowDownload(ppt.getAllowDownload() == 1)
                .uploaderId(ppt.getCreatedBy())
                .uploaderName(getUploaderName(ppt.getCreatedBy()))
                .status(mapStatusToString(ppt.getStatus()))
                .uploadedAt(ppt.getUploadedAt())
                .publishedAt(ppt.getPublishedAt())
                .updatedAt(ppt.getUpdatedAt())
                .build();
    }

    private String getUploaderName(Long userId) {
        if (userId == null) return "未知用户";
        User user = userMapper.selectById(userId);
        return user != null ? user.getRealName() : "未知用户";
    }

    private String formatFileSize(Long size) {
        if (size == null || size == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    @Async
    public void asyncDeleteOssFile(String url) {
        if (ossClient == null || ossProperties == null) {
            log.warn("OSS 未配置，跳过文件删除: {}", url);
            return;
        }
        try {
            String key = extractKeyFromUrl(url);
            ossClient.deleteObject(ossProperties.getBucketName(), key);
            log.info("OSS 文件删除成功: {}", key);
        } catch (Exception e) {
            log.error("OSS 文件删除失败: {}", url, e);
        }
    }
}