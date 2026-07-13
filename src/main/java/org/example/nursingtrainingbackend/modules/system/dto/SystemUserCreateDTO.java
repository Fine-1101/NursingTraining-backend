package org.example.nursingtrainingbackend.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SystemUserCreateDTO {

    @NotBlank
    @Size(min = 2, max = 20)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Size(max = 50)
    private String realName;

    @Size(max = 20)
    private String phone;

    private Long deptId;

    private Integer roleType = 1;

    private Integer status = 1;
}
