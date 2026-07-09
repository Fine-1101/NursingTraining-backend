package org.example.nursingtrainingbackend.modules.category.vo;

public record CategorySummary(
        long totalCategories,
        long enabledCategories,
        long disabledCategories,
        long totalCourses
) {}