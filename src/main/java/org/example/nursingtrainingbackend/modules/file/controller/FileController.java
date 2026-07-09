package org.example.nursingtrainingbackend.modules.file.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.file.dto.UploadPolicyRequest;
import org.example.nursingtrainingbackend.modules.file.service.FileService;
import org.example.nursingtrainingbackend.modules.file.vo.FileUploadResponse;
import org.example.nursingtrainingbackend.modules.file.vo.UploadPolicyResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    /**
     * 上传类型到目录的映射
     */
    private static final Map<String, String> UPLOAD_TYPE_TO_DIRECTORY = Map.of(
            "ARTICLE_COVER", "articles/covers",
            "ARTICLE_ATTACHMENT", "articles/attachments",
            "PPT_FILE", "ppts",
            "VIDEO_FILE", "videos",
            "VIDEO_COVER", "videos/covers"
    );

    @PostMapping("/upload")
    public Result<FileUploadResponse> upload(@RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String uploadType,
            @RequestParam(defaultValue = "files") @Pattern(regexp = "^[a-zA-Z0-9/_-]{1,64}$") String directory) {
        // 如果提供了 uploadType，则使用映射的目录；否则使用 directory 参数
        String targetDirectory = directory;
        if (uploadType != null && !uploadType.isBlank()) {
            targetDirectory = UPLOAD_TYPE_TO_DIRECTORY.getOrDefault(uploadType, directory);
        }
        return Result.success(fileService.upload(file, targetDirectory));
    }

    @PostMapping("/policy")
    public Result<UploadPolicyResponse> policy(@Valid @RequestBody UploadPolicyRequest request) {
        return Result.success(fileService.createPolicy(request));
    }
}
