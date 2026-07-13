package org.example.nursingtrainingbackend.modules.file.service;

import org.example.nursingtrainingbackend.modules.file.dto.UploadPolicyRequest;
import org.example.nursingtrainingbackend.modules.file.vo.FileUploadResponse;
import org.example.nursingtrainingbackend.modules.file.vo.UploadPolicyResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadResponse upload(MultipartFile file, String directory);

    FileUploadResponse upload(MultipartFile file, String directory, Authentication authentication);
    UploadPolicyResponse createPolicy(UploadPolicyRequest request);

    /**
     * 标记文件已绑定业务（在业务数据保存成功后调用）
     *
     * @param objectKey OSS对象Key
     * @param bizType   业务类型：COURSE_COVER, VIDEO, ARTICLE, PPT, AVATAR等
     * @param bizId     业务数据ID
     */
    void markFileUsed(String objectKey, String bizType, Long bizId);
}
