package org.example.nursingtrainingbackend.modules.assessment.service;

import jakarta.servlet.http.HttpServletResponse;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.assessment.dto.ResultQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultDetailVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultSummaryVO;

public interface AssessmentResultService {

    PageResult<ResultItemVO> listResults(ResultQueryDTO query);

    ResultDetailVO getResultDetail(Long attemptId);

    ResultSummaryVO getResultSummary(Long assessmentId);

    void exportResults(ResultQueryDTO query, HttpServletResponse response);
}
