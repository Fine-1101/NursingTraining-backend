package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

@Data
public class OptionVO {
    private String optionKey;
    private String content;
    private Boolean isCorrect;
    private Integer sortOrder;
}
