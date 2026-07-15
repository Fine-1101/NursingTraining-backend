// 文件路径: src/main/java/org/example/nursingtrainingbackend/modules/category/vo/CategoryNode.java
package org.example.nursingtrainingbackend.modules.category.vo;

import org.example.nursingtrainingbackend.modules.category.entity.Category;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record CategoryNode(
        Long id,
        String name,
        Long parentId,
        String parentName,
        Integer level,
        String icon,
        Integer status,
        long directCourseCount,
        long courseCount,
        boolean hasChildren,
        List<CategoryNode> children,
        LocalDateTime updatedAt
) {
    public static CategoryNode from(Category category, String parentName, boolean hasChildren,
                                     long directCourseCount, long courseCount) {
        return new CategoryNode(
                category.getId(),
                category.getName(),
                category.getParentId(),
                parentName,
                category.getLevel(),
                category.getIcon(),
                category.getStatus(),
                directCourseCount,
                courseCount,
                hasChildren,
                new ArrayList<>(),
                category.getUpdatedAt()
        );
    }
}