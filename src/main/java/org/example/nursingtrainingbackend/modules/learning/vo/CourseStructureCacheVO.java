package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 课程结构缓存VO：仅包含课程静态元数据（章节/知识点/资源标题等），不含用户进度。
 * 缓存键：nursing:course:study:v1:{courseId}
 * TTL：20分钟
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseStructureCacheVO implements Serializable {

    /** 课程ID */
    private Long courseId;
    /** 课程标题 */
    private String title;
    /** 课程简介 */
    private String summary;
    /** 封面URL */
    private String coverUrl;
    /** 类别名称 */
    private String categoryName;
    /** 启用课程点总数 */
    private Integer totalPointCount;
    /** 第一个有效课程点ID（用于导航） */
    private Long firstPointId;
    /** 章节列表（含知识点和资源元数据） */
    private List<ChapterItem> chapters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterItem implements Serializable {
        private Long chapterId;
        private String title;
        private Integer sort;
        private List<PointItem> points;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointItem implements Serializable {
        private Long pointId;
        private String title;
        private String description;
        private Boolean required;
        private Integer sort;
        private List<ResourceItem> resources;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceItem implements Serializable {
        /** ARTICLE / VIDEO / PPT */
        private String resourceType;
        private Long resourceId;
        private String title;
        /** 视频时长（秒），仅VIDEO有值 */
        private Integer durationSeconds;
    }
}
