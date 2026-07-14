package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

@Data
public class CourseItemVO {
    /**
     * 课程 ID
     */
    private Long id;

    /**
     * 课程封面
     */
    private String coverUrl;

    /**
     * 课程名称
     */
    private String title;

    /**
     * 类别 ID
     */
    private Long categoryId;

    /**
     * 类别名称
     */
    private String categoryName;

    /**
     * 讲师用户 ID
     */
    private Long instructorId;

    /**
     * 讲师姓名
     */
    private String instructorName;

    /**
     * 覆盖部门内当前有效学员数
     */
    private Long studentCount;

    /**
     * 发布状态（枚举字符串）
     */
    private String status;

    /**
     * 更新时间 datetime字符串
     */
    private String updatedAt;
}
