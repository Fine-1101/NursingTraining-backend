package org.example.nursingtrainingbackend.modules.file.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.common.utils.FileNameUtils;
import org.example.nursingtrainingbackend.config.properties.UploadProperties;
import org.example.nursingtrainingbackend.modules.file.dto.UploadPolicyRequest;
import org.example.nursingtrainingbackend.modules.file.service.FileService;
import org.example.nursingtrainingbackend.modules.file.vo.FileUploadResponse;
import org.example.nursingtrainingbackend.modules.file.vo.UploadPolicyResponse;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储实现 — 当 OSS 未配置时作为默认 FileService
 * 通过 FileStorageConfig 配置类注册 Bean
 */
@Slf4j
public class LocalFileServiceImpl implements FileService {

    private final UploadProperties uploadProperties;
    private final String localDirectory;
    private final String localBaseUrl;
    /** 初始化服务实现及其运行依赖。 */

    public LocalFileServiceImpl(UploadProperties uploadProperties, Environment env) {
        this.uploadProperties = uploadProperties;
        this.localDirectory = env.getProperty("app.upload.local-directory", "./uploads");
        this.localBaseUrl = env.getProperty("app.upload.local-base-url", "/uploads");
    }
    /** 上传并保存文件。 */

    @Override
    public FileUploadResponse upload(MultipartFile file, String directory) {
        validate(file.getSize(), file.getContentType(), file.isEmpty());
        String objectKey = FileNameUtils.objectKey("nursing-training", directory, file.getOriginalFilename());

        try {
            Path targetPath = Paths.get(localDirectory, objectKey);
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件已保存到本地: {}", targetPath);

            String url = localBaseUrl + "/" + objectKey;
            return new FileUploadResponse(objectKey, url, file.getOriginalFilename(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            log.error("本地文件保存失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, ErrorCode.FILE_UPLOAD_FAILED.getMessage(), e);
        }
    }
    /** 上传并保存文件。 */

    @Override
    public FileUploadResponse upload(MultipartFile file, String directory, Authentication authentication) {
        return null;
    }
    /** 创建客户端直传文件所需的上传策略。 */

    @Override
    public UploadPolicyResponse createPolicy(UploadPolicyRequest request) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "本地存储不支持客户端直传（Policy），请使用服务端上传");
    }
    /** 将已上传文件标记为被指定业务使用。 */

    @Override
    public void markFileUsed(String objectKey, String bizType, Long bizId) {

    }

    private void validate(long size, String contentType, boolean empty) {
        if (empty) throw new BusinessException(ErrorCode.FILE_EMPTY);
        if (size > uploadProperties.maxSize()) throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        if (contentType == null || !uploadProperties.allowedContentTypes().contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }
}
