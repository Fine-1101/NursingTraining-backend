package org.example.nursingtrainingbackend.modules.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompletionRuleDTO {
    @NotBlank(message = "完成规则不能为空")
    private String completionRule;
}
