package org.example.nursingtrainingbackend.modules.ppt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePptRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;

    @Size(max = 2000, message = "简介不能超过2000个字符")
    private String description;

    private Boolean allowDownload;
}
