package org.example.nursingtrainingbackend.modules.message.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MessageSendVO {
    private Long messageId;
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseTitle;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "GMT+8")
    private OffsetDateTime createdAt;
}
