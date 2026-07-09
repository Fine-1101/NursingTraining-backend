package org.example.nursingtrainingbackend.modules.category.vo;

import java.time.LocalDateTime;

public record RecentUpdateItem(
        Long categoryId,
        String categoryPath,
        LocalDateTime updatedAt
) {}