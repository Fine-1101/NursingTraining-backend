package org.example.nursingtrainingbackend.modules.courseware.ppt.service;

public interface PptConversionService {

    void convertAsync(Long pptId, String originalUrl, String originalName);
}
