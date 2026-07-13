package org.example.nursingtrainingbackend.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentUpdateDTO {

    @NotBlank
    @Size(max = 50)
    private String realName;

    @NotBlank
    @Size(max = 50)
    private String username;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不合法")
    private String phone;

    @Size(max = 500)
    private String avatarUrl;

    @Size(max = 255)
    private String avatarObjectKey;

    private Long departmentId;

    @NotBlank
    private String status;
}
