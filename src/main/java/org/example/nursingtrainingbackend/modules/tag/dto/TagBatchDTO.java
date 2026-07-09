package org.example.nursingtrainingbackend.modules.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class TagBatchDTO {

    @NotNull
    @Size(min = 1, max = 100)
    private List<Long> ids;

    @NotBlank
    @Pattern(regexp = "^(ENABLE|DISABLE|DELETE)$")
    private String action;
}
