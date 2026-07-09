// 文件路径: src/main/java/org/example/nursingtrainingbackend/modules/category/dto/CategoryTreeQuery.java
package org.example.nursingtrainingbackend.modules.category.dto;

public record CategoryTreeQuery(
        String keyword,
        Integer status,
        Long parentId
) {}