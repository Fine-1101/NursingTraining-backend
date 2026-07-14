package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;
import java.util.List;

@Data
public class CoursePointDetailVO {

    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String description;
    private Boolean required;
    private Integer sort;

    private List<MediaItem> articles;
    private List<MediaItem> videos;
    private List<MediaItem> ppts;

    @Data
    public static class MediaItem {
        private Long id;
        private String title;
        private String status;
        private Integer duration;       // 仅 video
        private Integer pageCount;      // 仅 ppt
    }
}
