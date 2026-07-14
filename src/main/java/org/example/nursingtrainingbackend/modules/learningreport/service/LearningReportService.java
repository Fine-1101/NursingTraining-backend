package org.example.nursingtrainingbackend.modules.learningreport.service;

import org.example.nursingtrainingbackend.modules.learningreport.dto.CreateReportRequest;
import org.example.nursingtrainingbackend.modules.learningreport.vo.CreateReportVO;

public interface LearningReportService {
    public CreateReportVO createReport(
            CreateReportRequest request
    );
}
