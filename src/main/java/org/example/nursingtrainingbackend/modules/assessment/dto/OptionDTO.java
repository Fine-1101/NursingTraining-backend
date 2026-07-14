package org.example.nursingtrainingbackend.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OptionDTO {

    @NotBlank
    @Size(max = 8)
    private String optionKey;

    @NotBlank
    @Size(max = 1000)
    private String content;

    private Boolean isCorrect;

    private Integer sortOrder;
}
