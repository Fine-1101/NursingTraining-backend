package org.example.nursingtrainingbackend.modules.course.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdateChapterOrder {
    @NotEmpty(message = "章节排序列表不能为空")
    private List<Long> chapterIds;
}
