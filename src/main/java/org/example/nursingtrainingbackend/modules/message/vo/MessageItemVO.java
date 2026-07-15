package org.example.nursingtrainingbackend.modules.message.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MessageItemVO {
    private Long messageId;
    private Long courseId;
    private String courseTitle;
    private String content;
    private String senderName;
    private Boolean read;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "GMT+8")
    private OffsetDateTime readAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "GMT+8")
    private OffsetDateTime createdAt;
}
