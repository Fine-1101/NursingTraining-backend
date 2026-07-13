package org.example.nursingtrainingbackend.modules.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentListItemVO {

    private Long studentId;
    private String avatarUrl;
    private String realName;
    private String username;
    private Long departmentId;
    private String departmentName;
    private BigDecimal averageProgressPercent;
    private String maskedPhone;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
