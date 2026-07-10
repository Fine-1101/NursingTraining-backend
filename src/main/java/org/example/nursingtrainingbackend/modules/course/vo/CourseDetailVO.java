package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.util.List;

@Data
public class CourseDetailVO {

    private Long id;

    private String title;

    private String summary;

    private String learningObjective;

    private Long categoryId;

    private String coverUrl;

    private Long instructorId;

    private String startAt;

    private List<Long> tagIds;

    private List<DepartmentItem> departments;

    private List<ChapterItem> chapters;

    private String status;

    private Integer currentStep;

    private String completionRule;

    private Object ruleSummary;

    @Data
    public static class DepartmentItem {
        private Long departmentId;
        private Boolean required;
    }

    @Data
    public static class ChapterItem {
        private Long id;
        private String title;
        private Integer sort;
        private List<PointItem> points;
    }

    @Data
    public static class PointItem {
        private Long id;
        private String title;
        private String description;
        private Boolean required;
        private Integer sort;
        private Integer resourceCount;
        private Integer articleCount;
        private Integer videoCount;
        private Integer pptCount;
    }
}
