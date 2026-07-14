package org.example.nursingtrainingbackend.modules.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class QuestionSaveDTO {

    @NotNull
    private Long categoryId;

    /** 1-单选题 2-判断题 */
    @NotNull
    private Integer questionType;

    @NotBlank
    @Size(max = 2000)
    private String stem;

    private String analysis;

    /** 1-简单 2-中等 3-困难 */
    @NotNull
    @Min(1)
    @Max(3)
    private Integer difficulty;

    /** 0-停用 1-启用，默认1 */
    private Integer status;

    @NotEmpty
    @Valid
    private List<OptionDTO> options;

    /** 空数组表示类别通用；非空表示仅指定课程可用 */
    private List<Long> courseIds;
}
