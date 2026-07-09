package org.example.nursingtrainingbackend.modules.tag.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class TagStatisticsVO {

    private Long totalAssociations;

    private Integer totalTags;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "GMT+8")
    private Instant generatedAt;

    private List<TagStatisticsItemVO> items;
}
