package org.example.nursingtrainingbackend.modules.file.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PolicyConditions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.common.utils.FileNameUtils;
import org.example.nursingtrainingbackend.config.OssConfig;
import org.example.nursingtrainingbackend.config.properties.UploadProperties;
import org.example.nursingtrainingbackend.modules.file.dto.UploadPolicyRequest;
import org.example.nursingtrainingbackend.modules.file.entity.FileUploadRecord;
import org.example.nursingtrainingbackend.modules.file.mapper.FileUploadRecordMapper;
import org.example.nursingtrainingbackend.modules.file.service.FileService;
import org.example.nursingtrainingbackend.modules.file.vo.FileUploadResponse;
import org.example.nursingtrainingbackend.modules.file.vo.UploadPolicyResponse;
import org.example.nursingtrainingbackend.config.condition.OssEnabledCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Primary
@Service
@Conditional(OssEnabledCondition.class)
@RequiredArgsConstructor
public class OssFileServiceImpl implements FileService {

    private final OSS ossClient;
    private final UploadProperties uploadProperties;
    private final OssConfig ossConfig;
    private final FileUploadRecordMapper fileUploadRecordMapper;

    private static final String BASE_DIRECTORY = "nursing-training";
    private static final long COVER_MAX_SIZE = 2 * 1024 * 1024L;

    private static final Map<String, Set<String>> DIRECTORY_ALLOWED_EXTENSIONS = Map.of(
            "videos/originals", Set.of(".mp4"),
            "videos/covers", Set.of(".jpg", ".jpeg", ".png")
    );

    private static final Map<String, Set<String>> DIRECTORY_ALLOWED_CONTENT_TYPES = Map.of(
            "videos/originals", Set.of("video/mp4"),
            "videos/covers", Set.of("image/jpeg", "image/png")
    );

    @Override
    public FileUploadResponse upload(MultipartFile file, String directory) {
        return null;
    }

    @Override
    public FileUploadResponse upload(MultipartFile file, String directory, Authentication authentication) {
        validate(file.getSize(), file.getContentType(), file.isEmpty());
        String key = FileNameUtils.objectKey(BASE_DIRECTORY, directory, file.getOriginalFilename());
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            ossClient.putObject(ossConfig.getBucketName(), key, file.getInputStream(), metadata);

            // 鍐欏叆涓婁紶璁板綍
            FileUploadRecord record = new FileUploadRecord();
            record.setObjectKey(key);
            record.setUrl(publicUrl(key));
            record.setUploadType("DIRECT");
            record.setOriginalFileName(file.getOriginalFilename());
            record.setSize(file.getSize());
            record.setContentType(file.getContentType());
            record.setUsed(0);
            if (authentication != null && authentication.getPrincipal() instanceof org.example.nursingtrainingbackend.security.AuthenticatedUser user) {
                record.setCreatedBy(user.id());
            }
            fileUploadRecordMapper.insert(record);

            return new FileUploadResponse(key, publicUrl(key), file.getOriginalFilename(), file.getSize(), file.getContentType());
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, ErrorCode.FILE_UPLOAD_FAILED.getMessage(), exception);
        }
    }

    @Override
    public UploadPolicyResponse createPolicy(UploadPolicyRequest request) {
        String ext = FileNameUtils.extension(request.fileName());
        if (ext.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "鏂囦欢鎵╁睍鍚嶄笉鍚堟硶");
        }

        validateDirectoryConstraints(request.directory(), "." + ext, request.contentType());

        long maxSize = resolveMaxSize(request.directory());
        validate(0, request.contentType(), false);

        String key = FileNameUtils.objectKey(BASE_DIRECTORY, request.directory(), request.fileName());
        Instant expiration = Instant.now().plus(Duration.ofMinutes(5));

        PolicyConditions conditions = new PolicyConditions();
        conditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 1, maxSize);
        conditions.addConditionItem("Content-Type", request.contentType());
        conditions.addConditionItem(PolicyConditions.COND_KEY, key);

        String policyText = ossClient.generatePostPolicy(Date.from(expiration), conditions);
        String policy = Base64.getEncoder().encodeToString(policyText.getBytes(StandardCharsets.UTF_8));
        String signature = ossClient.calculatePostSignature(policyText);

        return new UploadPolicyResponse(uploadHost(), key, policy, signature,
                ossConfig.getAccessKeyId(), request.contentType(), expiration.toEpochMilli());
    }

    private void validateDirectoryConstraints(String directory, String extension, String contentType) {
        Set<String> allowedExtensions = DIRECTORY_ALLOWED_EXTENSIONS.get(directory);
        if (allowedExtensions != null && !allowedExtensions.contains(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "鐩綍 " + directory + " 涓嶆敮鎸佹枃浠剁被鍨?" + extension);
        }

        Set<String> allowedContentTypes = DIRECTORY_ALLOWED_CONTENT_TYPES.get(directory);
        if (allowedContentTypes != null && !allowedContentTypes.contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "鐩綍 " + directory + " 涓嶆敮鎸?Content-Type: " + contentType);
        }
    }

    private long resolveMaxSize(String directory) {
        if ("videos/covers".equals(directory)) {
            return COVER_MAX_SIZE;
        }
        return uploadProperties.maxSize();
    }

    private void validate(long size, String contentType, boolean empty) {
        if (empty) throw new BusinessException(ErrorCode.FILE_EMPTY);
        if (size > uploadProperties.maxSize()) throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        if (contentType == null || !uploadProperties.allowedContentTypes().contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private String uploadHost() {
        String ep = ossConfig.getEndpoint().replaceFirst("^https?://", "");
        return "https://" + ossConfig.getBucketName() + "." + ep;
    }

    private String publicUrl(String key) {
        return uploadHost() + "/" + key;
    }

    @Override
    public void markFileUsed(String objectKey, String bizType, Long bizId) {
        LambdaQueryWrapper<FileUploadRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileUploadRecord::getObjectKey, objectKey)
                .eq(FileUploadRecord::getUsed, 0)
                .isNull(FileUploadRecord::getDeletedAt);
        FileUploadRecord record = fileUploadRecordMapper.selectOne(wrapper);
        if (record == null) {
            // 璁板綍涓嶅瓨鍦ㄦ垨宸蹭娇鐢紝闈欓粯蹇界暐锛堥伩鍏嶄笟鍔′繚瀛樺け璐ワ級
            return;
        }
        record.setUsed(1);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setUsedAt(LocalDateTime.now());
        fileUploadRecordMapper.updateById(record);
    }
}