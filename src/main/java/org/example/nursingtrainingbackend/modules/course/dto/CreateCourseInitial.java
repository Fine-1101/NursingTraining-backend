package org.example.nursingtrainingbackend.modules.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;
import java.util.Map;

@Data
public class CreateCourseInitial {
    /**
     * 课程名称，去除首尾空格后长度1～50
     */
    @NotBlank(message = "课程名称不能为空")
    @Length(min = 1, max = 50, message = "课程名称长度1-50个字符")
    private String title;

    /**
     * 课程简介，长度1～500
     */
    @NotBlank(message = "课程简介不能为空")
    @Length(min = 1, max = 500, message = "课程简介长度1-500个字符")
    private String summary;

    /**
     * 学习目标，长度1～300
     */
    @NotBlank(message = "学习目标不能为空")
    @Length(min = 1, max = 300, message = "学习目标长度1-300个字符")
    private String learningObjective;

    /**
     * 启用且未删除的类别ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * OSS封面地址，必须以 courses/covers/ 开头
     */
    @NotBlank(message = "封面地址不能为空")
    private String coverUrl;

    /**
     * 标签ID，0~3个，不可重复
     */
    @Size(max = 3, message = "最多选择3个标签")
    private List<Long> tagIds;

    /**
     * 启用讲师ID
     */
  @NotNull(message = "讲师ID不能为空")
    private Long instructorId;

    /**
     * 开课时间，可选
     */
    private String startAt;

    /**
     * 发布部门配置，至少1条，嵌套DTO校验
     */
    @NotNull(message = "发布部门不能为空")
    @Valid // 开启嵌套对象校验
    private List<DepartmentDTO> departments;
}
