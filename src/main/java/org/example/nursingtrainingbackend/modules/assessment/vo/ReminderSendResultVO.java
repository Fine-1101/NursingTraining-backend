package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

@Data
public class ReminderSendResultVO {

    private Long assessmentId;
    private Long courseId;
    private Integer requestedCount;
    private Integer sentCount;
    private Integer skippedCount;
    private Integer failedCount;
    private String batchId;
}
