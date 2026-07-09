package org.example.nursingtrainingbackend.modules.tag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagUpdateDTO {

    @NotBlank
    @Size(min = 1, max = 50)
    private String name;

    @NotBlank
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String color;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status;
}
