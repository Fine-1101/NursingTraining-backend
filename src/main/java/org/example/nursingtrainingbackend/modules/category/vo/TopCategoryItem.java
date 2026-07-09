package org.example.nursingtrainingbackend.modules.category.vo;

public record TopCategoryItem(
        Long categoryId,
        String categoryName,
        long courseCount,
        int rank
) {}