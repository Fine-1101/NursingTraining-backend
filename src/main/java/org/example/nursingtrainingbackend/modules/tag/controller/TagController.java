package org.example.nursingtrainingbackend.modules.tag.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.tag.dto.TagBatchDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagCreateDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagQueryDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagStatusDTO;
import org.example.nursingtrainingbackend.modules.tag.dto.TagUpdateDTO;
import org.example.nursingtrainingbackend.modules.tag.service.TagService;
import org.example.nursingtrainingbackend.modules.tag.vo.TagBatchResultVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagItemVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagOverviewVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagStatisticsVO;
import org.example.nursingtrainingbackend.modules.tag.vo.TagStatusVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public Result<PageResult<TagItemVO>> queryPage(@Valid TagQueryDTO query) {
        return Result.success(tagService.queryPage(query));
    }

    @GetMapping("/overview")
    public Result<TagOverviewVO> getOverview() {
        return Result.success(tagService.getOverview());
    }

    @GetMapping("/statistics")
    public Result<TagStatisticsVO> getStatistics() {
        return Result.success(tagService.getStatistics());
    }

    @GetMapping("/{id}")
    public Result<TagItemVO> getDetail(@PathVariable Long id) {
        return Result.success(tagService.getDetail(id));
    }

    @PostMapping
    public Result<TagItemVO> createTag(@RequestBody @Valid TagCreateDTO dto) {
        return Result.success(tagService.createTag(dto));
    }

    @PutMapping("/{id}")
    public Result<TagItemVO> updateTag(@PathVariable Long id,
                                       @RequestBody @Valid TagUpdateDTO dto) {
        return Result.success(tagService.updateTag(id, dto));
    }

    @PatchMapping("/{id}/status")
    public Result<TagStatusVO> updateStatus(@PathVariable Long id,
                                            @RequestBody @Valid TagStatusDTO dto) {
        return Result.success(tagService.updateStatus(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }

    @PostMapping("/batch")
    public Result<TagBatchResultVO> batchOperate(@RequestBody @Valid TagBatchDTO dto) {
        return Result.success(tagService.batchOperate(dto));
    }
}
