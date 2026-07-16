package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReminderItemVO {

    private Long messageId;
    private Long assessmentId;
    private String assessmentTitle;
    private Long courseId;
    private String courseTitle;
    private Long receiverId;
    private String receiverUsername;
    private String receiverName;
    private Long departmentId;
    private String departmentName;
    private Long senderId;
    private String senderName;
    private String batchId;
    private String content;
    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;
}
