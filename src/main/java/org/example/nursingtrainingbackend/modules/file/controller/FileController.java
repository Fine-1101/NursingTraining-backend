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

@Validated
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/upload")
    public Result<FileUploadResponse> upload(@RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "files") @Pattern(regexp = "^[a-zA-Z0-9/_-]{1,64}$") String directory) {
        return Result.success(fileService.upload(file, directory));
    }

    @PostMapping("/policy")
    public Result<UploadPolicyResponse> policy(@Valid @RequestBody UploadPolicyRequest request) {
        return Result.success(fileService.createPolicy(request));
    }
}
