package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCompleteVO {

    private Long articleId;

    private String learningStatus;

    private BigDecimal progressPercent;

    private Boolean completed;

    private Boolean pointCompleted;

    private Boolean courseCompleted;
}
