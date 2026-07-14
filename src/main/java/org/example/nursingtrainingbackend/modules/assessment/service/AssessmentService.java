package org.example.nursingtrainingbackend.modules.assessment.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.assessment.dto.AssessmentCreateRequest;
import org.example.nursingtrainingbackend.modules.assessment.dto.AssessmentListQuery;
import org.example.nursingtrainingbackend.modules.assessment.vo.*;

public interface AssessmentService {
    /**
     * 查询考核列表（分页）
     */
    PageResult<AssessmentListItemVO> listAssessments(AssessmentListQuery query);

    /**
     * 创建考核草稿
     */
    AssessmentCreateResponseVO createAssessment(AssessmentCreateRequest request);

    /**
     * 查询考核详情
     */
    AssessmentDetailVO getAssessmentDetail(Long assessmentId);

    /**
     * 11. 修改考核草稿
     */
    void updateAssessment(Long assessmentId, AssessmentCreateRequest request);

    /**
     * 12. 发布前检查题量
     */
    QuestionCapacityVO checkQuestionCapacity(Long assessmentId);

    /**
     * 13. 发布考核
     */
    PublishAssessmentVO publishAssessment(Long assessmentId);

    /**
     * 14. 关闭考核
     */
    CloseAssessmentVO closeAssessment(Long assessmentId);

    /**
     * 15. 删除考核草稿
     */
    void deleteAssessment(Long assessmentId);
}
