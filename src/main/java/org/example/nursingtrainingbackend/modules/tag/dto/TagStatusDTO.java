package org.example.nursingtrainingbackend.modules.tag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TagStatusDTO {

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status;
}
