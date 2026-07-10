package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.util.List;

@Data
public class UpdateChapterOrderVO {
    private List<Long> chapterIds;
    private Integer affectedCount;
}
