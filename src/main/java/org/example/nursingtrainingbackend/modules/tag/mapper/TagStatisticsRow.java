package org.example.nursingtrainingbackend.modules.tag.mapper;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TagStatisticsRow {

    private Long id;

    private String name;

    private String color;

    private Long courseCount;

    private Long totalAssociations;

    private BigDecimal usageRate;
}
