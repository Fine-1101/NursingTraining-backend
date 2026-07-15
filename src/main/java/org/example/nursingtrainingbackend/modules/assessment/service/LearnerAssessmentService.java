package org.example.nursingtrainingbackend.modules.assessment.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.assessment.dto.LearnerResultHistoryQuery;
import org.example.nursingtrainingbackend.modules.assessment.vo.learner.*;

import java.util.List;

/**
 * 学员端考核服务
 */
public interface LearnerAssessmentService {

    PageResult<AssessmentResultHistoryItemVO> listResultHistory(
            Long userId,
            LearnerResultHistoryQuery query);

    /**
     * 查询课程考核卡片
     */
    AssessmentCardVO getAssessmentCard(Long courseId, Long userId);

    List<AssessmentCardVO> listAssessmentCards(Long courseId, Long userId);

    /**
     * 开始或继续考试
     */
    StartAttemptVO startOrResumeAttempt(Long assessmentId, Long userId);

    /**
     * 获取试卷详情
     */
    AttemptPaperVO getAttemptPaper(Long attemptId, Long userId);

    /**
     * 保存单题答案
     */
    SaveAnswerResponse saveAnswer(Long attemptId, Long attemptQuestionId, Long userId, SaveAnswerRequest request);

    /**
     * 交卷
     */
    SubmitAttemptVO submitAttempt(Long attemptId, Long userId);

    /**
     * 查看成绩详情
     */
    AttemptResultVO getAttemptResult(Long attemptId, Long userId);

    AttemptReviewVO getAttemptReview(Long attemptId, Long userId);

    int autoSubmitExpiredAttempts();
}
