package org.example.nursingtrainingbackend.modules.courseware.ppt.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PptDetailVO {
    private Long id;
    private String title;
    private String description;
    private String originalName;
    private Long fileSize;
    private Long courseCount;
    private Boolean allowDownload;
    private Long uploaderId;
    private String uploaderName;
    private String status;
    private LocalDateTime uploadedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}