package org.example.nursingtrainingbackend.modules.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentDetailVO {

    private Long studentId;
    private String avatarUrl;
    private String realName;
    private String username;
    private String phone;
    private Long departmentId;
    private String departmentName;
    private String roleType;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    private Integer courseCount;
    private BigDecimal averageProgressPercent;
}
