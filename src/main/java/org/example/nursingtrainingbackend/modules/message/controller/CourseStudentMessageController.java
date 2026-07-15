package org.example.nursingtrainingbackend.modules.message.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.annotation.RateLimit;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.message.dto.MessageSendRequest;
import org.example.nursingtrainingbackend.modules.message.entity.CourseStudentMessage;
import org.example.nursingtrainingbackend.modules.message.service.CourseStudentMessageService;
import org.example.nursingtrainingbackend.modules.message.vo.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CourseStudentMessageController {

    private final CourseStudentMessageService messageService;

    @RateLimit(key = "admin:send:message", time = 60, count = 60, limitType = RateLimit.LimitType.USER)
    @PostMapping("/api/admin/settings/students/{studentId}/courses/{courseId}/messages")
    public Result<MessageSendVO> sendCourseMessage(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            @RequestBody @Valid MessageSendRequest request) {
        CourseStudentMessage message = messageService.sendCourseMessage(studentId, courseId, request);
        return Result.success(messageService.buildSendVO(message, studentId));
    }

    @GetMapping("/api/messages")
    public Result<PageResult<MessageItemVO>> listMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ALL") String readStatus) {
        return Result.success(messageService.selectMessagePage(page, size, readStatus));
    }

    @GetMapping("/api/messages/unread-count")
    public Result<UnreadCountVO> getUnreadCount() {
        return Result.success(messageService.getUnreadCount());
    }

    @PatchMapping("/api/messages/{messageId}/read")
    public Result<MarkReadVO> markAsRead(@PathVariable Long messageId) {
        return Result.success(messageService.markAsRead(messageId));
    }

    @PatchMapping("/api/messages/read-all")
    public Result<MarkAllReadVO> readAll() {
        return Result.success(messageService.readAllMessages());
    }
}
