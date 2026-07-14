package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionListItemVO {

    private Long id;
    private String stem;
    private Integer questionType;
    private String questionTypeName;
    private Long categoryId;
    private String categoryName;
    private Integer difficulty;
    private String difficultyName;
    private String scope;
    private String scopeName;
    private List<Long> courseIds;
    private List<String> courseNames;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** GROUP_CONCAT 原始字符串，由 MyBatis 自动映射，业务层解析后使用 */
    private String courseIdsStr;
    /** GROUP_CONCAT 原始字符串，由 MyBatis 自动映射，业务层解析后使用 */
    private String courseNamesStr;
}
