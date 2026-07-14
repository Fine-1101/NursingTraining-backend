package org.example.nursingtrainingbackend.modules.courseware.video.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoBatchPublishResponseVO {

    private Integer requestedCount;

    private Integer publishedCount;

    private LocalDateTime publishedAt;
}
