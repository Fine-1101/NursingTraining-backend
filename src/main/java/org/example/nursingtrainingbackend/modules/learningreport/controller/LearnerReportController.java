package org.example.nursingtrainingbackend.modules.learningreport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.learningreport.dto.CreateReportRequest;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportFeedbackRequest;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportPageQuery;
import org.example.nursingtrainingbackend.modules.learningreport.dto.RegenerateReportRequest;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;
import org.example.nursingtrainingbackend.modules.learningreport.service.LearningReportService;
import org.example.nursingtrainingbackend.modules.learningreport.vo.CreateReportVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.LearningReportDetailVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.LearningReportListItemVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.RegenerateReportVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.ReportEligibilityVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.SubmitReportFeedbackVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learner/learning-reports")
@RequiredArgsConstructor
public class LearnerReportController {

    private final LearningReportService learningReportService;

    @GetMapping("/eligibility")
    public Result<ReportEligibilityVO> getEligibility(
            @RequestParam(defaultValue = "WEEKLY") ReportType reportType,
            @RequestParam(required = false) Long courseId
    ) {
        return Result.success(
                learningReportService.getEligibility(reportType, courseId)
        );
    }

    @PostMapping
    public ResponseEntity<Result<CreateReportVO>> createReport(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReportRequest request
    ) {
        CreateReportVO data = learningReportService.createReport(
                request,
                idempotencyKey
        );
        return ResponseEntity.accepted().body(Result.success(data));
    }

    @GetMapping("/current")
    public Result<LearningReportDetailVO> getCurrent(
            @RequestParam(defaultValue = "WEEKLY") ReportType reportType,
            @RequestParam(required = false) Long courseId
    ) {
        return Result.success(
                learningReportService.getCurrent(reportType, courseId)
        );
    }

    @GetMapping("/{reportId}")
    public Result<LearningReportDetailVO> getDetail(
            @PathVariable Long reportId
    ) {
        return Result.success(learningReportService.getDetail(reportId));
    }

    @GetMapping
    public Result<PageResult<LearningReportListItemVO>> list(
            @Valid LearningReportPageQuery query
    ) {
        return Result.success(learningReportService.list(query));
    }

    @PostMapping("/{reportId}/regenerate")
    public ResponseEntity<Result<RegenerateReportVO>> regenerate(
            @PathVariable Long reportId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody(required = false) RegenerateReportRequest request
    ) {
        RegenerateReportVO data = learningReportService.regenerate(
                reportId,
                request,
                idempotencyKey
        );
        return ResponseEntity.accepted().body(Result.success(data));
    }

    @PostMapping("/{reportId}/feedback")
    public Result<SubmitReportFeedbackVO> submitFeedback(
            @PathVariable Long reportId,
            @Valid @RequestBody LearningReportFeedbackRequest request
    ) {
        return Result.success(
                learningReportService.submitFeedback(reportId, request)
        );
    }
}
