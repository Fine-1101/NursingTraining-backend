// 文件路径: src/main/java/org/example/nursingtrainingbackend/modules/category/vo/CategoryTreeResult.java
package org.example.nursingtrainingbackend.modules.category.vo;

import java.util.List;

public record CategoryTreeResult(List<CategoryNode> categories, long total) {}