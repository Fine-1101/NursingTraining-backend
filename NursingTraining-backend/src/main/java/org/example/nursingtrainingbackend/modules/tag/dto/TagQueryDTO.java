package org.example.nursingtrainingbackend.modules.tag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TagQueryDTO {

    private String keyword;

    @Min(0)
    @Max(1)
    private Integer status;

    @Pattern(regexp = "^(updatedAt|createdAt|courseCount|name)$")
    private String sortBy = "updatedAt";

    @Pattern(regexp = "^(asc|desc)$")
    private String sortOrder = "desc";

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer size = 20;
}
