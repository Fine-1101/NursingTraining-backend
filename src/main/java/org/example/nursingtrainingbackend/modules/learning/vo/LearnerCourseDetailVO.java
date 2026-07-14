package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学员端课程学习详情VO（学习页面使用）
 */
@Data
public class LearnerCourseDetailVO {

    /** 课程ID */
    private Long courseId;

    /** 课程名称 */
    private String title;

    /** 课程简介 */
    private String summary;

    /** 课程封面URL */
    private String coverUrl;

    /** 课程类别名称 */
    private String categoryName;

    /** 课程类型：REQUIRED-必修，OPTIONAL-选修 */
    private String courseType;

    /** 学习状态：NOT_STARTED、LEARNING、COMPLETED */
    private String learningStatus;

    /** 当前进度百分比 */
    private BigDecimal progressPercent;

    /** 总课程点数 */
    private Integer totalPointCount;

    /** 已完成课程点数 */
    private Integer completedPointCount;

    /** 当前学习课程点ID */
    private Long currentPointId;

    /** 按钮文案 */
    private String buttonText;

    /** 章节列表 */
    private List<ChapterVO> chapters;

    @Data
    public static class ChapterVO {
        /** 章节ID */
        private Long chapterId;

        /** 章节标题 */
        private String title;

        /** 章节排序 */
        private Integer sort;

        /** 课程点列表 */
        private List<PointVO> points;
    }

    @Data
    public static class PointVO {
        /** 课程点ID */
        private Long pointId;

        /** 课程点标题 */
        private String title;

        /** 课程点描述 */
        private String description;

        /** 是否必修 */
        private Boolean required;

        /** 排序 */
        private Integer sort;

        /** 学习状态：NOT_STARTED、LEARNING、COMPLETED */
        private String learningStatus;

        /** 关联课件列表 */
        private List<ResourceVO> resources;
    }

    @Data
    public static class ResourceVO {
        /** 课件类型：ARTICLE、VIDEO、PPT */
        private String resourceType;

        /** 课件ID */
        private Long resourceId;

        /** 课件标题 */
        private String title;

        /** 学习状态：NOT_STARTED、LEARNING、COMPLETED */
        private String learningStatus;

        /** 进度百分比 */
        private BigDecimal progressPercent;

        /** 视频时长（秒），仅VIDEO类型 */
        private Integer durationSeconds;

        /** 视频当前播放位置（秒），仅VIDEO类型 */
        private Integer lastPositionSeconds;
    }
}
