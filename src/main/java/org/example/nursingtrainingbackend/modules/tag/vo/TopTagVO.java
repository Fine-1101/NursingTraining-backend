package org.example.nursingtrainingbackend.modules.tag.vo;

import lombok.Data;

@Data
public class TopTagVO {

    private Long tagId;

    private String tagName;

    private String color;

    private Long courseCount;

    private Integer rank;
}
