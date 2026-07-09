package org.example.nursingtrainingbackend.modules.tag.vo;

import java.util.List;
import lombok.Data;

@Data
public class TagOverviewVO {

    private Long totalTags;

    private Long enabledTags;

    private Long disabledTags;

    private Long usedTags;

    private Long unusedTags;

    private List<TopTagVO> topTags;
}
