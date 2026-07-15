package org.example.nursingtrainingbackend.modules.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageSendRequest(
        @NotBlank(message = "消息内容长度必须为1到1000个字符")
        @Size(max = 1000, message = "消息内容长度必须为1到1000个字符")
        String content
) {
}