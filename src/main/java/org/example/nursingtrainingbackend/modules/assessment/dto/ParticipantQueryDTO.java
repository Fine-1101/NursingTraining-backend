package org.example.nursingtrainingbackend.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ParticipantQueryDTO {

    private String participationStatus;

    @Size(max = 100, message = "关键词最长100字符")
    private String keyword;

    private Long departmentId;

    @Min(value = 1, message = "页码从1开始")
    private Long page = 1L;

    @Min(value = 1, message = "每页最少1条")
    @Max(value = 100, message = "每页最多100条")
    private Long size = 20L;
}
