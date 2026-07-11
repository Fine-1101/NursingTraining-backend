package org.example.nursingtrainingbackend.modules.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class GetCourseDTO {
    /**
     * 搜索关键词：模糊匹配课程名称/讲师姓名
     * 去除首尾空格后长度 1~100，允许不传
     */
    @Length(min = 1, max = 100, message = "关键词去除首尾空格后长度必须在1-100之间")
    private String keyword;

    /**
     * 类别ID，传入则校验分类存在且未删除（数据库业务校验，注解仅做非数值拦截）
     */
    private Long categoryId;

    /**
     * 发布状态，仅允许 DRAFT / PUBLISHED / OFFLINE
     */
    @Pattern(regexp = "^(DRAFT|PUBLISHED|OFFLINE)?$", message = "状态仅支持 DRAFT、PUBLISHED、OFFLINE")
    private String status;

    /**
     * 当前页码，默认1，最小1
     */
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    /**
     * 每页条数，默认10，范围1~100
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Integer size = 10;
}
