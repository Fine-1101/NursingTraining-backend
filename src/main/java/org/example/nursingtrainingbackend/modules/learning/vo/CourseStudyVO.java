package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 课程学习页 VO
 */
@Data
public class CourseStudyVO {
    /** 当前课程概要 */
    private CourseSummaryVO course;
    /** 当前学习课程点 */
    private CurrentPointVO currentPoint;
    /** 视频/文章/PPT资源分组 */
    private ResourceTabVO tabs;
    /** 上下课程点导航 */
    private NavigationVO navigation;

    /**
     * 课程概要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseSummaryVO {
        /** 课程 ID */
        private Long courseId;
        /** 课程名称 */
        private String title;
        /** 课程封面 URL，可为空 */
        private String coverUrl;
        /** 当前学员课程进度 */
        private BigDecimal progressPercent;
        /** 已完成课程点数量 */
        private Integer completedPointCount;
        /** 启用、未删除课程点总数 */
        private Integer pointCount;
        /** 讲师姓名 */
        private String instructorName;
        /** 讲师所属科室 */
        private String instructorDepartment;
        /** 课程标签列表 */
        private List<String> tags;
    }

    /**
     * 当前学习课程点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentPointVO {
        /** 课程点 ID */
        private Long pointId;
        /** 所属章节 ID */
        private Long chapterId;
        /** 所属章节名称 */
        private String chapterTitle;
        /** 课程点名称 */
        private String title;
        /** 课程点简介，可为空 */
        private String description;
        /** 是否必修课程点 */
        private Boolean required;
        /** 当前学员课程点状态 */
        private String learningStatus;
        /** 默认激活 Tab */
        private String activeType;
    }

    /**
     * 资源Tab容器：封装视频、文章、PPT数组
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceTabVO {
        /** 当前课程点下的视频列表 */
        private List<VideoVO> videos;
        /** 当前课程点下的文章列表 */
        private List<ArticleVO> articles;
        /** 当前课程点下的 PPT 列表 */
        private List<PptVO> ppts;
    }

    /**
     * 视频资源VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoVO {
        /** 视频 ID */
        private Long videoId;
        /** 视频标题 */
        private String title;
        /** 视频简介，可为空 */
        private String description;
        /** 视频封面，可为空 */
        private String coverUrl;
        /** 在线播放地址 */
        private String playUrl;
        /** 视频总时长，单位秒，可为空 */
        private Integer durationSeconds;
        /** 是否允许拖拽进度条 */
        private Boolean allowDrag;
        /** 是否允许倍速播放 */
        private Boolean allowSpeed;
        /** 当前学员视频学习状态 */
        private String learningStatus;
        /** 视频学习进度 */
        private BigDecimal progressPercent;
        /** 最近停留位置，用于续播，可为空 */
        private Integer lastPositionSeconds;
        /** 历史最远播放位置，用于完成判断，可为空 */
        private Integer maxPositionSeconds;
        /** 是否完成 */
        private Boolean completed;
    }

    /**
     * 文章资源VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleVO {
        /** 文章 ID */
        private Long articleId;
        /** 文章标题 */
        private String title;
        /** 文章摘要，可为空 */
        private String summary;
        /** 清洗后的 HTML 富文本正文 */
        private String htmlContent;
        /** 附件名称，可为空 */
        private String attachmentName;
        /** 附件在线预览地址，可为空 */
        private String attachmentPreviewUrl;
        /** 是否允许下载附件 */
        private Boolean allowDownload;
        /** 当前学员文章学习状态 */
        private String learningStatus;
        /** 文章学习进度；完成后为 100 */
        private BigDecimal progressPercent;
        /** 是否完成 */
        private Boolean completed;
    }

    /**
     * PPT资源VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PptVO {
        /** PPT ID */
        private Long pptId;
        /** PPT 标题 */
        private String title;
        /** PPT 简介，可为空 */
        private String description;
        /** PPT 封面，可为空 */
        private String coverUrl;
        /** 在线预览地址 */
        private String previewUrl;
        /** PPT 页数，可为空 */
        private Integer pageCount;
        /** 是否允许下载原文件 */
        private Boolean allowDownload;
        /** 当前学员 PPT 学习状态 */
        private String learningStatus;
        /** PPT 学习进度；完成后为 100 */
        private BigDecimal progressPercent;
        /** 是否完成 */
        private Boolean completed;
    }

    /**
     * 上下课程点导航
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavigationVO {
        /** 上一个课程点 ID，可为空 */
        private Long previousPointId;
        /** 下一个课程点 ID，可为空 */
        private Long nextPointId;
    }
}
