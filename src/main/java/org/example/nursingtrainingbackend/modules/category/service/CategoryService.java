// 文件路径: src/main/java/org/example/nursingtrainingbackend/modules/category/service/CategoryService.java
package org.example.nursingtrainingbackend.modules.category.service;

import java.util.List;

import org.example.nursingtrainingbackend.modules.category.dto.CategoryCreateRequest;
import org.example.nursingtrainingbackend.modules.category.dto.CategoryEditRequest;
import org.example.nursingtrainingbackend.modules.category.dto.CategoryStatusRequest;
import org.example.nursingtrainingbackend.modules.category.dto.CategoryTreeQuery;
import org.example.nursingtrainingbackend.modules.category.vo.*;

public interface CategoryService {
    CategoryTreeResult getTree(CategoryTreeQuery query);
    CategoryVO getDetail(Long id);
    CategoryVO create(CategoryCreateRequest request);
    CategoryEditVO update(Long id, CategoryEditRequest request);
    CategoryStatusVO updateStatus(Long id, CategoryStatusRequest request);
    void delete(Long id);
    BatchDeleteVO batchDelete(List<Long> ids);
    CategoryOverviewResult getOverview();
}
