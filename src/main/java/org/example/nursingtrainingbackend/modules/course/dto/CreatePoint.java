package org.example.nursingtrainingbackend.modules.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreatePoint {
    /**
     * 路径参数：课程ID
     */
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    /**
     * 路径参数：章节ID
     */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 课程点名称，1~50字符
     */
    @NotBlank(message = "课程点名称不能为空")
    @Size(min = 1, max = 50, message = "课程点名称长度必须在1-50个字符之间")
    private String title;

    /**
     * 简介，最长500，允许不传
     */
    @Size(max = 500, message = "简介不能超过500个字符")
    private String description;

    /**
     * 是否必修，必填
     */
    @NotNull(message = "必修标识不能为空")
    private Boolean required;

    /**
     * 已发布文章ID数组，允许空，不能重复
     */
    private List<Long> articleIds;

    /**
     * 可用视频ID数组，允许空，不能重复
     */
    private List<Long> videoIds;

    /**
     * 已发布PPT ID数组，允许空，不能重复
     */
    private List<Long> pptIds;
}
