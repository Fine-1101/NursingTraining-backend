package org.example.nursingtrainingbackend.modules.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 学员端分页查询请求DTO
 */
@Data
public class LearnerPageQuery {

    /** 当前页码，默认1 */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    /** 每页条数，默认10，最大50 */
    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 50, message = "每页条数不能超过50")
    private Integer size = 10;
}
