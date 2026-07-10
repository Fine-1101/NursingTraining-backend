package org.example.nursingtrainingbackend.modules.courseware.video.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoListItemVO {

    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    private Integer duration;

    private String durationText;

    private Long fileSize;

    private String fileSizeText;

    private Long uploaderId;

    private String uploaderName;

    private LocalDateTime uploadedAt;

    private String status;
}
