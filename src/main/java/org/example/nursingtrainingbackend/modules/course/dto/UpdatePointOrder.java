package org.example.nursingtrainingbackend.modules.course.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePointOrder {
    @NotEmpty(message = "课程点排序列表不能为空")
    private List<Long> pointIds;
}
