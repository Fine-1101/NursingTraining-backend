// 文件路径: src/main/java/org/example/nursingtrainingbackend/modules/category/controller/CategoryController.java
package org.example.nursingtrainingbackend.modules.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.category.dto.*;
import org.example.nursingtrainingbackend.modules.category.service.CategoryService;
import org.example.nursingtrainingbackend.modules.category.vo.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping("/tree")
    public Result<CategoryTreeResult> tree(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "0") Long parentId
    ) {
        return Result.success(categoryService.getTree(new CategoryTreeQuery(keyword, status, parentId)));
    }

    @GetMapping("/overview")
    public Result<CategoryOverviewResult> overview() {
        return Result.success(categoryService.getOverview());
    }

    @PostMapping
    public Result<CategoryVO> create(@Valid @RequestBody CategoryCreateRequest request) {
        return Result.success(categoryService.create(request));
    }

    @GetMapping("/{id}")
    public Result<CategoryVO> detail(@PathVariable Long id) {
        return Result.success(categoryService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<CategoryEditVO> update(@PathVariable Long id, @Valid @RequestBody CategoryEditRequest request) {
        return Result.success(categoryService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public Result<CategoryStatusVO> updateStatus(@PathVariable Long id, @Valid @RequestBody CategoryStatusRequest request) {
        return Result.success(categoryService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<BatchDeleteVO> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
        return Result.success(categoryService.batchDelete(request.ids()));
    }

}
