package org.example.nursingtrainingbackend.modules.file.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PolicyConditions;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.common.utils.FileNameUtils;
import org.example.nursingtrainingbackend.config.properties.OssProperties;
import org.example.nursingtrainingbackend.config.properties.UploadProperties;
import org.example.nursingtrainingbackend.modules.file.dto.UploadPolicyRequest;
import org.example.nursingtrainingbackend.modules.file.service.FileService;
import org.example.nursingtrainingbackend.modules.file.vo.FileUploadResponse;
import org.example.nursingtrainingbackend.modules.file.vo.UploadPolicyResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class OssFileServiceImpl implements FileService {
    private final OssProperties ossProperties;
    private final UploadProperties uploadProperties;

    @Override
    public FileUploadResponse upload(MultipartFile file, String directory) {
        validate(file.getSize(), file.getContentType(), file.isEmpty());
        requireConfigured();
        String key = FileNameUtils.objectKey(ossProperties.baseDirectory(), directory, file.getOriginalFilename());
        OSS client = createClient();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            client.putObject(ossProperties.bucketName(), key, file.getInputStream(), metadata);
            return new FileUploadResponse(key, publicUrl(key), file.getOriginalFilename(), file.getSize(), file.getContentType());
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, ErrorCode.FILE_UPLOAD_FAILED.getMessage(), exception);
        } finally {
            client.shutdown();
        }
    }

    @Override
    public UploadPolicyResponse createPolicy(UploadPolicyRequest request) {
        validate(0, request.contentType(), false);
        requireConfigured();
        String key = FileNameUtils.objectKey(ossProperties.baseDirectory(), request.directory(), request.fileName());
        Instant expiration = Instant.now().plus(ossProperties.policyExpiration());
        OSS client = createClient();
        try {
            PolicyConditions conditions = new PolicyConditions();
            conditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 1, uploadProperties.maxSize());
            conditions.addConditionItem(PolicyConditions.COND_KEY, key);
            conditions.addConditionItem("Content-Type", request.contentType());
            String policyText = client.generatePostPolicy(Date.from(expiration), conditions);
            String policy = Base64.getEncoder().encodeToString(policyText.getBytes(StandardCharsets.UTF_8));
            String signature = client.calculatePostSignature(policyText);
            return new UploadPolicyResponse(uploadHost(), key, policy, signature, ossProperties.accessKeyId(),
                    request.contentType(), expiration.toEpochMilli());
        } finally {
            client.shutdown();
        }
    }

    private void validate(long size, String contentType, boolean empty) {
        if (empty) throw new BusinessException(ErrorCode.FILE_EMPTY);
        if (size > uploadProperties.maxSize()) throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        if (contentType == null || !uploadProperties.allowedContentTypes().contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private void requireConfigured() {
        if (!ossProperties.configured()) throw new BusinessException(ErrorCode.OSS_NOT_CONFIGURED);
    }

    private OSS createClient() {
        return new OSSClientBuilder().build(ossProperties.endpoint(), ossProperties.accessKeyId(), ossProperties.accessKeySecret());
    }

    private String uploadHost() {
        String endpoint = ossProperties.endpoint().replaceFirst("^https?://", "");
        return "https://" + ossProperties.bucketName() + "." + endpoint;
    }

    private String publicUrl(String key) {
        String domain = ossProperties.publicDomain();
        String base = domain == null || domain.isBlank() ? uploadHost() : domain.replaceAll("/+$", "");
        if (!base.startsWith("http://") && !base.startsWith("https://")) base = "https://" + base;
        return base + "/" + key;
    }
}
