package org.example.nursingtrainingbackend.modules.assessment.service;

import org.example.nursingtrainingbackend.modules.assessment.vo.learner.*;

/**
 * 学员端考核服务
 */
public interface LearnerAssessmentService {

    /**
     * 查询课程考核卡片
     */
    AssessmentCardVO getAssessmentCard(Long courseId, Long userId);

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
}
