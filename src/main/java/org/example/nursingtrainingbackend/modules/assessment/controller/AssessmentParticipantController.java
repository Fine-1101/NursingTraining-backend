package org.example.nursingtrainingbackend.modules.assessment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.assessment.dto.ParticipantQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.dto.ReminderQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.dto.ReminderSendRequest;
import org.example.nursingtrainingbackend.modules.assessment.service.AssessmentParticipantService;
import org.example.nursingtrainingbackend.modules.assessment.vo.ParticipantItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ReminderItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ReminderSendResultVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AssessmentParticipantController {

    private final AssessmentParticipantService participantService;

    /** 查询考核参与人员分页 */
    @GetMapping("/api/admin/assessments/{assessmentId}/participants")
    public Result<PageResult<ParticipantItemVO>> listParticipants(
            @PathVariable Long assessmentId,
            @Valid ParticipantQueryDTO query) {
        return Result.success(participantService.listParticipants(assessmentId, query));
    }

    /** 提醒未参加考核人员 */
    @PostMapping("/api/admin/assessments/{assessmentId}/non-participant-reminders")
    public Result<ReminderSendResultVO> sendReminders(
            @PathVariable Long assessmentId,
            @Valid @RequestBody ReminderSendRequest request) {
        return Result.success(participantService.sendReminders(assessmentId, request));
    }

    /** 查询考核提醒发送历史 */
    @GetMapping("/api/admin/assessments/{assessmentId}/reminders")
    public Result<PageResult<ReminderItemVO>> listReminders(
            @PathVariable Long assessmentId,
            @Valid ReminderQueryDTO query) {
        return Result.success(participantService.listReminders(assessmentId, query));
    }
}
