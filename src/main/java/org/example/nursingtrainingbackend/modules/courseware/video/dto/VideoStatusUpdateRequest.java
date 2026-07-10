package org.example.nursingtrainingbackend.modules.courseware.video.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VideoStatusUpdateRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}
