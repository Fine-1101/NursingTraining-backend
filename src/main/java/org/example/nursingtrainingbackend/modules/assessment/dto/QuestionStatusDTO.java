package org.example.nursingtrainingbackend.modules.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionStatusDTO {

    @NotNull
    private Integer status;
}
