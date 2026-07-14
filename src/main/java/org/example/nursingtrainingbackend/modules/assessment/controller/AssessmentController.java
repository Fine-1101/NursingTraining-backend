package org.example.nursingtrainingbackend.modules.assessment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.assessment.dto.AssessmentCreateRequest;
import org.example.nursingtrainingbackend.modules.assessment.dto.AssessmentListQuery;
import org.example.nursingtrainingbackend.modules.assessment.service.AssessmentService;
import org.example.nursingtrainingbackend.modules.assessment.vo.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 考核管理控制器
 */
@RestController
@RequestMapping("/api/admin/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    /**
     * 8. 查询考核列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<AssessmentListItemVO>> listAssessments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTo,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size
    ) {
        AssessmentListQuery query = new AssessmentListQuery(
                keyword, courseId, categoryId, status, startFrom, startTo, page, size
        );
        PageResult<AssessmentListItemVO> result = assessmentService.listAssessments(query);
        return Result.success(result);
    }

    /**
     * 9. 创建考核草稿
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AssessmentCreateResponseVO> createAssessment(
            @Valid @RequestBody AssessmentCreateRequest request
    ) {
        AssessmentCreateResponseVO result = assessmentService.createAssessment(request);
        return Result.success(result);
    }

    /**
     * 10. 查询考核详情
     */
    @GetMapping("/{assessmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AssessmentDetailVO> getAssessmentDetail(@PathVariable Long assessmentId) {
        AssessmentDetailVO result = assessmentService.getAssessmentDetail(assessmentId);
        return Result.success(result);
    }

    /**
     * 11. 修改考核草稿
     */
    @PutMapping("/{assessmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateAssessment(
            @PathVariable Long assessmentId,
            @Valid @RequestBody AssessmentCreateRequest request
    ) {
        assessmentService.updateAssessment(assessmentId, request);
        return Result.success();
    }

    /**
     * 12. 发布前检查题量
     */
    @GetMapping("/{assessmentId}/question-capacity")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<QuestionCapacityVO> checkQuestionCapacity(@PathVariable Long assessmentId) {
        QuestionCapacityVO result = assessmentService.checkQuestionCapacity(assessmentId);
        return Result.success(result);
    }

    /**
     * 13. 发布考核
     */
    @PostMapping("/{assessmentId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PublishAssessmentVO> publishAssessment(@PathVariable Long assessmentId) {
        PublishAssessmentVO result = assessmentService.publishAssessment(assessmentId);
        return Result.success(result);
    }

    /**
     * 14. 关闭考核
     */
    @PostMapping("/{assessmentId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CloseAssessmentVO> closeAssessment(@PathVariable Long assessmentId) {
        CloseAssessmentVO result = assessmentService.closeAssessment(assessmentId);
        return Result.success(result);
    }

    /**
     * 15. 删除考核草稿
     */
    @DeleteMapping("/{assessmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteAssessment(@PathVariable Long assessmentId) {
        assessmentService.deleteAssessment(assessmentId);
        return Result.success();
    }
}
