package org.example.nursingtrainingbackend.modules.courseware.video.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoStatusUpdateResponseVO {

    private Long id;

    private String status;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;
}
