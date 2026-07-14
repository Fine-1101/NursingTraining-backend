package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuestionDetailVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Integer questionType;
    private String stem;
    private String analysis;
    private Integer difficulty;
    private Integer status;
    private List<OptionVO> options;
    private String scope;
    private List<Long> courseIds;
    private List<String> courseNames;
}
