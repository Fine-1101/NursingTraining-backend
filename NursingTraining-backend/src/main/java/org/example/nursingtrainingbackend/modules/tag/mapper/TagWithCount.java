package org.example.nursingtrainingbackend.modules.tag.mapper;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TagWithCount {

    private Long id;

    private String name;

    private String color;

    private Integer status;

    private Long courseCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
