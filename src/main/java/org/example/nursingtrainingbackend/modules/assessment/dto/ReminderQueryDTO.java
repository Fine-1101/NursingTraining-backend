package org.example.nursingtrainingbackend.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ReminderQueryDTO {

    @Size(max = 100, message = "关键词最长100字符")
    private String keyword;

    private String readStatus;

    private String batchId;

    private Long senderId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime sentFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime sentTo;

    @Min(value = 1, message = "页码从1开始")
    private Long page = 1L;

    @Min(value = 1, message = "每页最少1条")
    @Max(value = 100, message = "每页最多100条")
    private Long size = 20L;
}
