package org.example.nursingtrainingbackend.modules.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentDeleteVO {

    private Long studentId;
    private Boolean deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedAt;
}
