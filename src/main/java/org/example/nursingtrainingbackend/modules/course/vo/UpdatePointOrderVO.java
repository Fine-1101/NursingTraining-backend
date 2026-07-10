package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.util.List;

@Data
public class UpdatePointOrderVO {
    private List<Long> pointIds;
    private Integer affectedCount;
}
