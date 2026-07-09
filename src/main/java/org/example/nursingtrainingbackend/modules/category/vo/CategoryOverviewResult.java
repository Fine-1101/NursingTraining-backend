package org.example.nursingtrainingbackend.modules.category.vo;

import java.util.List;

public record CategoryOverviewResult(
        CategorySummary summary,
        List<TopCategoryItem> topCategories,
        List<RecentUpdateItem> recentUpdates
) {}