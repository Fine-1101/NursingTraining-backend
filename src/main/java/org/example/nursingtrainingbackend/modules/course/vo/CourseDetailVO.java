package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data

public class CourseDetailVO {
    private Long courseId;
    private String title;
    private String summary;
    private String learningObjective;
    private Long categoryId;
    private String coverUrl;
    private List<Long> tagIds;
    private Long instructorId;
    private LocalDateTime startAt;
    private List<DepartmentVO> departments;
    private String instructorName;
    private String status;
    private String completionRule;
    private Integer currentStep;
    private RuleSummaryVO ruleSummary;
    private List<ChapterVO> chapters;

    @Data
    public static class DepartmentVO {
        private Long departmentId;
        private String departmentName;
        private Boolean required;
    }

    @Data
    public static class RuleSummaryVO {
        private Integer requiredPointCount;
        private Integer optionalPointCount;
        private Integer articleCount;
        private Integer videoCount;
        private Integer pptCount;
        private Boolean structureValid;
        private List<String> validationErrors;
    }

    @Data
    public static class ChapterVO {
        private Long id;
        private String title;
        private Integer sort;
        private List<PointVO> points;
    }

    @Data
    public static class PointVO {
        private Long id;
        private String title;
        private String description;
        private Boolean required;
        private Integer sort;
        private Integer articleCount;
        private Integer videoCount;
        private Integer pptCount;
        private List<ArticleSummaryVO> articles;
        private List<VideoSummaryVO> videos;
        private List<PptSummaryVO> ppts;
    }

    @Data
    public static class ArticleSummaryVO {
        private Long id;
        private String title;
        private String coverUrl;
    }

    @Data
    public static class VideoSummaryVO {
        private Long id;
        private String title;
        private String coverUrl;
        private Integer duration;
    }

    @Data
    public static class PptSummaryVO {
        private Long id;
        private String title;
        private String coverUrl;
    }
}
