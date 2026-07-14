package org.example.nursingtrainingbackend.modules.courseware.video.vo;

import lombok.Data;

@Data
public class VideoOverviewVO {

    private Long totalVideos;

    private Long storageBytes;

    private String storageText;

    private Long publishedVideos;

    private Long draftVideos;

    private MonthOverMonth monthOverMonth;

    @Data
    public static class MonthOverMonth {
        private Double totalVideosRate;
        private Double storageRate;
        private Double publishedVideosRate;
        private Double draftVideosRate;
    }
}
