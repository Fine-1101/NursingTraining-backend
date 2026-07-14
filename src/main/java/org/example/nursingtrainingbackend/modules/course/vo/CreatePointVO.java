package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

@Data
public class CreatePointVO {

    // 课程点名称
    private String title;
    // 是否必修
    private Boolean required;
    // 排序号，新增自动追加到章节末尾
    private Integer sort;
    // 文章数量
    private Integer articleCount;
    // 视频数量
    private Integer videoCount;
    // PPT数量
    private Integer pptCount;
    // 三类课件总和
    private Integer resourceCount;
}
