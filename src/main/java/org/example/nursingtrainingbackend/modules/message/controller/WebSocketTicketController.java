package org.example.nursingtrainingbackend.modules.message.controller;

import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.message.service.WebSocketTicketService;
import org.example.nursingtrainingbackend.modules.message.vo.TicketVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ws")
@RequiredArgsConstructor
public class WebSocketTicketController {

    private final WebSocketTicketService ticketService;

    @PostMapping("/ticket")
    public Result<TicketVO> createTicket() {
        return Result.success(ticketService.createTicket());
    }
}
