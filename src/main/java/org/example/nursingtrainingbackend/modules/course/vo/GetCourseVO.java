package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.util.List;

@Data
public class GetCourseVO {
    /**
     * 当前页课程列表
     */
    private List<CourseItemVO> records;

    /**
     * 符合条件的课程总数
     */
    private Long total;

    /**
     * 当前页
     */
    private Integer page;

    /**
     * 每页数量
     */
    private Integer size;

    /**
     * 总页数
     */
    private Integer pages;
}
