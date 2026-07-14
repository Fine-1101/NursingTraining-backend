package org.example.nursingtrainingbackend.modules.courseware.video.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoUploadResponseVO {

    private Long id;

    private String title;

    private Long uploaderId;

    private String uploaderName;

    private String status;

    private LocalDateTime uploadedAt;

    private LocalDateTime publishedAt;
}
