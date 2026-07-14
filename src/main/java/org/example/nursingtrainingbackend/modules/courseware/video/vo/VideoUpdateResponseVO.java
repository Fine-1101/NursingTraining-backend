package org.example.nursingtrainingbackend.modules.courseware.video.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoUpdateResponseVO {

    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    private Boolean allowDrag;

    private Boolean allowSpeed;

    private Boolean allowCache;

    private String status;

    private LocalDateTime updatedAt;
}
