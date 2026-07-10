package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

@Data
public class CompletionRuleVO {
    private String completionRule;
    private Integer requiredPointCount;
    private Integer optionalPointCount;
    private Boolean structureValid;
}
