package org.example.nursingtrainingbackend.modules.assessment.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.assessment.dto.ParticipantQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.dto.ReminderQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.dto.ReminderSendRequest;
import org.example.nursingtrainingbackend.modules.assessment.vo.ParticipantItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ReminderItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ReminderSendResultVO;

public interface AssessmentParticipantService {

    /** 查询考核参与人员分页 */
    PageResult<ParticipantItemVO> listParticipants(Long assessmentId, ParticipantQueryDTO query);

    /** 提醒未参加考核人员 */
    ReminderSendResultVO sendReminders(Long assessmentId, ReminderSendRequest request);

    /** 查询考核提醒发送历史 */
    PageResult<ReminderItemVO> listReminders(Long assessmentId, ReminderQueryDTO query);
}
