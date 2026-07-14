package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionResultItemVO {

    private Integer number;
    private Integer questionType;
    private String stem;

    private List<SimpleOptionVO> options;

    private String selectedOptionKey;
    private String correctOptionKey;
    private Boolean correct;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String analysis;

    @Data
    public static class SimpleOptionVO {
        private String optionKey;
        private String content;
    }
}
