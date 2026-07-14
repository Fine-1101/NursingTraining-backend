package org.example.nursingtrainingbackend.modules.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UploadPolicyRequest(
        @NotBlank(message = "文件名不能为空")
        @Size(max = 255, message = "文件名不能超过255个字符")
        String fileName,

        @NotBlank(message = "Content-Type不能为空")
        @Size(max = 150, message = "Content-Type不能超过150个字符")
        String contentType,

        @NotBlank(message = "目录不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9/_-]{1,64}$", message = "目录格式不合法")
        String directory
) {}