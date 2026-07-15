package org.example.nursingtrainingbackend.modules.assessment.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.assessment.dto.ResultQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.service.AssessmentResultService;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultDetailVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultSummaryVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AssessmentResultController {

    private final AssessmentResultService resultService;

    /** API-16: 查询成绩列表 */
    @GetMapping("/api/admin/assessment-results")
    public Result<PageResult<ResultItemVO>> listResults(@Valid ResultQueryDTO query) {
        return Result.success(resultService.listResults(query));
    }

    /** API-17: 查询单次成绩详情 */
    @GetMapping("/api/admin/assessment-results/{attemptId}")
    public Result<ResultDetailVO> getResultDetail(@PathVariable Long attemptId) {
        return Result.success(resultService.getResultDetail(attemptId));
    }

    /** API-18: 查询考核成绩概览 */
    @GetMapping("/api/admin/assessments/{assessmentId}/result-summary")
    public Result<ResultSummaryVO> getResultSummary(@PathVariable Long assessmentId) {
        return Result.success(resultService.getResultSummary(assessmentId));
    }

    /** API-19: 导出成绩 */
    @GetMapping("/api/admin/assessment-results/export")
    public void exportResults(@Valid ResultQueryDTO query, HttpServletResponse response) {
        resultService.exportResults(query, response);
    }
}
