package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ResultDetailVO {

    private Long attemptId;
    private Long assessmentId;
    private String assessmentTitle;
    private Long courseId;
    private String courseTitle;

    private UserInfoVO user;

    private BigDecimal score;
    private BigDecimal totalScore;
    private BigDecimal passScore;
    private Boolean passed;

    private Integer correctCount;
    private Integer wrongCount;
    private Integer unansweredCount;

    private List<QuestionResultItemVO> questions;

    @Data
    public static class UserInfoVO {
        private Long id;
        private String username;
        private String realName;
        private String departmentName;
    }
}
