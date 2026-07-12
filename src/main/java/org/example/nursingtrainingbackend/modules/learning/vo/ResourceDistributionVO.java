package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDistributionVO {

    private String resourceType;

    private String resourceTypeName;

    private Integer count;

    private BigDecimal percent;
}
