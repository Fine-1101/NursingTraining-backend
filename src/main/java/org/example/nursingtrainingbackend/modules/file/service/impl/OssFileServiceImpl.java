package org.example.nursingtrainingbackend.modules.file.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PolicyConditions;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.common.utils.FileNameUtils;
import org.example.nursingtrainingbackend.config.properties.UploadProperties;
import org.example.nursingtrainingbackend.modules.file.dto.UploadPolicyRequest;
import org.example.nursingtrainingbackend.modules.file.service.FileService;
import org.example.nursingtrainingbackend.modules.file.vo.FileUploadResponse;
import org.example.nursingtrainingbackend.modules.file.vo.UploadPolicyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.example.nursingtrainingbackend.config.condition.OssEnabledCondition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@Conditional(OssEnabledCondition.class)
@RequiredArgsConstructor
public class OssFileServiceImpl implements FileService {

    // ✅ 注入 OSS 客户端（由 OssConfig 提供）
    private final OSS ossClient;
    private final UploadProperties uploadProperties;

    // 从配置文件读取，如果读不到则使用默认值
    @Value("${oss.bucket-name:shared-doc-backend}")
    private String bucketName;

    @Value("${oss.endpoint:oss-cn-beijing.aliyuncs.com}")
    private String endpoint;

    private static final String BASE_DIRECTORY = "nursing-training";

    @Override
    public FileUploadResponse upload(MultipartFile file, String directory) {
        validate(file.getSize(), file.getContentType(), file.isEmpty());
        String key = FileNameUtils.objectKey(BASE_DIRECTORY, directory, file.getOriginalFilename());
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            // ✅ 直接使用 ossClient
            ossClient.putObject(bucketName, key, file.getInputStream(), metadata);
            return new FileUploadResponse(key, publicUrl(key), file.getOriginalFilename(), file.getSize(), file.getContentType());
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, ErrorCode.FILE_UPLOAD_FAILED.getMessage(), exception);
        }
    }

    @Override
    public UploadPolicyResponse createPolicy(UploadPolicyRequest request) {
        validate(0, request.contentType(), false);
        String key = FileNameUtils.objectKey(BASE_DIRECTORY, request.directory(), request.fileName());
        Instant expiration = Instant.now().plus(java.time.Duration.ofMinutes(5));
        try {
            PolicyConditions conditions = new PolicyConditions();
            conditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 1, uploadProperties.maxSize());
            conditions.addConditionItem(PolicyConditions.COND_KEY, key);
            conditions.addConditionItem("Content-Type", request.contentType());
            // ✅ 直接使用 ossClient
            String policyText = ossClient.generatePostPolicy(Date.from(expiration), conditions);
            String policy = Base64.getEncoder().encodeToString(policyText.getBytes(StandardCharsets.UTF_8));
            String signature = ossClient.calculatePostSignature(policyText);
            // 从 OssConfig 获取 accessKeyId（硬编码）
            String accessKeyId = "LTAI5t6PWNynkY6eVmDCXGXQ";
            return new UploadPolicyResponse(uploadHost(), key, policy, signature, accessKeyId,
                    request.contentType(), expiration.toEpochMilli());
        } finally {
            // ⚠️ 不要关闭 ossClient，它是单例 Bean，由 Spring 管理
        }
    }

    private void validate(long size, String contentType, boolean empty) {
        if (empty) throw new BusinessException(ErrorCode.FILE_EMPTY);
        if (size > uploadProperties.maxSize()) throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        if (contentType == null || !uploadProperties.allowedContentTypes().contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private String uploadHost() {
        String ep = endpoint.replaceFirst("^https?://", "");
        return "https://" + bucketName + "." + ep;
    }

    private String publicUrl(String key) {
        return uploadHost() + "/" + key;
    }
}