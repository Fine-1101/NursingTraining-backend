// 文件路径: src/main/java/org/example/nursingtrainingbackend/modules/category/dto/CategoryCreateRequest.java
package org.example.nursingtrainingbackend.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank @Size(max = 100) String name,
        Long parentId,
        @Size(max = 200) String icon,
        Integer status
) {}