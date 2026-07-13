package org.example.nursingtrainingbackend.modules.course.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreatePointDTO {
    private String title;
    private String description;
    private Boolean required;
    private List<Long> articleIds;
    private List<Long> videoIds;
    private List<Long> pptIds;
}
