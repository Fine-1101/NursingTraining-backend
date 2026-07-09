package org.example.nursingtrainingbackend.modules.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UploadPolicyRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 150) String contentType,
        @Pattern(regexp = "^[a-zA-Z0-9/_-]{1,64}$") String directory) {
    public UploadPolicyRequest {
        if (directory == null || directory.isBlank()) directory = "files";
    }
}
