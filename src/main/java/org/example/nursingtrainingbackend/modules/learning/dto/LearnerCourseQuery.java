package org.example.nursingtrainingbackend.modules.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 学员课程列表查询参数
 */
@Data
public class LearnerCourseQuery {

    /** 学习状态筛选：ALL / NOT_STARTED / LEARNING / COMPLETED */
    private String learningStatus = "ALL";

    /** 课程性质筛选：ALL / REQUIRED / OPTIONAL */
    private String courseType = "ALL";

    /** 关键词（课程名称或讲师姓名） */
    private String keyword;

    /** 当前页码，默认1 */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    /** 每页条数，默认10，最大50 */
    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 50, message = "每页条数不能超过50")
    private Integer size = 10;
}
