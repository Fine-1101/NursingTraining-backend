package org.example.nursingtrainingbackend.modules.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateChapter {
    @NotBlank(message = "章节标题不能为空")
    @Size(max = 1200, message = "章节标题长度不能超过1200")
    private String title;
}
