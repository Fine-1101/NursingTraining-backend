package org.example.nursingtrainingbackend.modules.courseware.video.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoDetailVO {

    private Long id;

    private String title;

    private String description;

    private String originalName;

    private String coverUrl;

    private Integer duration;

    private Long fileSize;

    private Boolean allowDrag;

    private Boolean allowSpeed;

    private Boolean allowCache;

    private Long viewCount;

    private Long watchCount;

    private Long uploaderId;

    private String uploaderName;

    private String status;

    private LocalDateTime uploadedAt;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;
}
