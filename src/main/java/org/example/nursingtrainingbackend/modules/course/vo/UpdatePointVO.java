package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UpdatePointVO {

    private Long id;

    private String title;

    private Boolean required;

    private Integer sort;

    private Integer articleCount;

    private Integer videoCount;

    private Integer pptCount;

    private Integer resourceCount;

    private LocalDateTime updatedAt;
}
