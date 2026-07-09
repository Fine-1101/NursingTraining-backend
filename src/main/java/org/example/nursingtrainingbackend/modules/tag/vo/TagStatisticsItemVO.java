package org.example.nursingtrainingbackend.modules.tag.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TagStatisticsItemVO {

    private Long tagId;

    private String tagName;

    private String color;

    private Long courseCount;

    private BigDecimal usageRate;

    private String usageRateText;

    private Integer sortRank;
}
