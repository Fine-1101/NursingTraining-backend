package org.example.nursingtrainingbackend.modules.category.vo;

import org.example.nursingtrainingbackend.modules.category.entity.Category;

import java.time.LocalDateTime;

public record CategoryEditVO(
        Long id,
        String name,
        Long parentId,
        Integer level,
        String icon,
        Integer status,
        int affectedCount,
        LocalDateTime updatedAt
) {
    public static CategoryEditVO from(Category category, int affectedCount) {
        return new CategoryEditVO(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.getLevel(),
                category.getIcon(),
                category.getStatus(),
                affectedCount,
                category.getUpdatedAt()
        );
    }
}
