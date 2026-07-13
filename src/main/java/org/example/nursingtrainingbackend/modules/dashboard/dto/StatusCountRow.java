package org.example.nursingtrainingbackend.modules.dashboard.dto;

import lombok.Data;

/** 学习状态分布查询结果行 */
@Data
public class StatusCountRow {
    private Integer status;
    private Integer cnt;
}
