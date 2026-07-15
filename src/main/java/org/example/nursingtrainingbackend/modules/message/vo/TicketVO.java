package org.example.nursingtrainingbackend.modules.message.vo;

import lombok.Data;

@Data
public class TicketVO {
    private String ticket;
    private Long expiresIn;
    private String webSocketPath;
}
