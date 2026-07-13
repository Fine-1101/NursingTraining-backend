package org.example.nursingtrainingbackend.modules.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class DepartmentDistributionVO {

    private Integer total;
    private List<DepartmentDistributionItemVO> items;
}
