package org.example.nursingtrainingbackend.modules.ppt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePptRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;

    @Size(max = 2000, message = "简介不能超过2000个字符")
    private String description;

    @NotBlank(message = "文件地址不能为空")
    private String originalUrl;

    @NotBlank(message = "原文件名不能为空")
    @Size(max = 255, message = "文件名不能超过255个字符")
    private String originalName;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    private Boolean allowDownload = false;

    private String status = "DRAFT";
}
