package org.example.nursingtrainingbackend.modules.courseware.ppt.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PptListItem {
    private Long id;
    private String title;
    private String originalName;
    private Long fileSize;
    private String fileSizeText;
    private Long courseCount;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime uploadedAt;
    private String status;
}
