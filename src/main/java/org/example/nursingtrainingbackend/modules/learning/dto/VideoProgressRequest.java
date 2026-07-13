package org.example.nursingtrainingbackend.modules.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VideoProgressRequest {

    @NotNull(message = "当前播放位置不能为空")
    @Min(value = 0, message = "当前播放位置不能小于0")
    private Integer currentSeconds;

    @NotNull(message = "视频总时长不能为空")
    @Positive(message = "视频总时长必须大于0")
    private Integer durationSeconds;

    @NotBlank(message = "进度事件类型不能为空")
    private String eventType;

    private Boolean ended = false;
}
