package org.example.nursingtrainingbackend.modules.assessment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReminderSendRequest {

    private List<Long> userIds;

    @NotNull(message = "remindAll 不能为空")
    private Boolean remindAll;

    @Size(max = 500, message = "备注内容最长500字符")
    private String content;
}
