package org.example.nursingtrainingbackend.modules.learningreport.service;

import org.example.nursingtrainingbackend.modules.learningreport.dto.CreateReportRequest;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportFeedbackRequest;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportPageQuery;
import org.example.nursingtrainingbackend.modules.learningreport.dto.RegenerateReportRequest;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.learningreport.vo.CreateReportVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.LearningReportDetailVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.LearningReportListItemVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.RegenerateReportVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.ReportEligibilityVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.SubmitReportFeedbackVO;

public interface LearningReportService {
    public CreateReportVO createReport(
            CreateReportRequest request,
            String idempotencyKey
    );

    ReportEligibilityVO getEligibility(ReportType reportType, Long courseId);

    LearningReportDetailVO getCurrent(ReportType reportType, Long courseId);

    LearningReportDetailVO getDetail(Long reportId);

    PageResult<LearningReportListItemVO> list(LearningReportPageQuery query);

    RegenerateReportVO regenerate(
            Long reportId,
            RegenerateReportRequest request,
            String idempotencyKey
    );

    SubmitReportFeedbackVO submitFeedback(
            Long reportId,
            LearningReportFeedbackRequest request
    );
}
