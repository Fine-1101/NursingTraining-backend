package org.example.nursingtrainingbackend.modules.courseware.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VideoUpdateRequest {

    @NotBlank(message = "视频标题不能为空")
    @Size(max = 1200, message = "视频标题长度不能超过1200")
    private String title;

    @Size(max = 2000, message = "视频描述长度不能超过2000")
    private String description;

    @Size(max = 500, message = "封面地址长度不能超过500")
    private String coverUrl;

    @NotNull(message = "是否允许拖拽不能为空")
    private Boolean allowDrag;

    @NotNull(message = "是否允许倍速不能为空")
    private Boolean allowSpeed;

    @NotNull(message = "是否允许缓存不能为空")
    private Boolean allowCache;
}
