package org.example.nursingtrainingbackend.modules.category.vo;

import org.example.nursingtrainingbackend.modules.category.entity.Category;

import java.time.LocalDateTime;

public record CategoryVO(
        Long id,
        String name,
        Long parentId,
        String parentName,
        Integer level,
        String icon,
        Integer status,
        long directCourseCount,
        long courseCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryVO from(Category category, String parentName) {
        return new CategoryVO(
                category.getId(),
                category.getName(),
                category.getParentId(),
                parentName,
                category.getLevel(),
                category.getIcon(),
                category.getStatus(),
                0L,
                0L,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
