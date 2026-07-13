package org.example.nursingtrainingbackend.modules.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SystemUserUpdateDTO {

    @Size(max = 50)
    private String realName;

    @Size(max = 20)
    private String phone;

    private Long deptId;

    private Integer roleType;
}
