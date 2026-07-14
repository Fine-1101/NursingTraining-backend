package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

/**
 * 课程导出行数据
 */
@Data
public class CourseExportRowVO {
    /** 课程名称 */
    private String title;
    /** 类别名称 */
    private String categoryName;
    /** 讲师姓名 */
    private String instructorName;
    /** 学员数 */
    private Long studentCount;
    /** 发布状态（中文） */
    private String status;
    /** 更新时间 yyyy-MM-dd HH:mm:ss */
    private String updatedAt;
}
