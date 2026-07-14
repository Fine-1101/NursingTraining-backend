package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.util.List;

@Data
public class CompletionRuleVO {

    private Long courseId;

    private String completionRule;

    private Integer requiredPointCount;

    private Integer optionalPointCount;

    private Integer articleCount;

    private Integer videoCount;

    private Integer pptCount;

    private List<DepartmentVO> departments;

    private Boolean structureValid;

    private List<String> validationErrors;

    private Integer currentStep;

    @Data
    public static class DepartmentVO {
        private Long departmentId;
        private String departmentName;
        private Boolean required;
    }
}
