package org.example.nursingtrainingbackend.modules.courseware.video.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class VideoBatchRequest {

    @NotEmpty(message = "视频ID列表不能为空")
    @Size(min = 1, max = 100, message = "视频ID数量必须在1-100之间")
    private List<Long> ids;
}
