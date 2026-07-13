package org.example.nursingtrainingbackend.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SystemUserStatusDTO {

    @Min(0)
    @Max(1)
    private Integer status;
}
