package org.example.nursingtrainingbackend.modules.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.assessment.service.LearnerAssessmentService;
import org.example.nursingtrainingbackend.modules.assessment.vo.learner.*;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 学员端考核控制器
 */
@RestController
@RequestMapping("/api/learner")
@RequiredArgsConstructor
public class LearnerAssessmentController {

    private final LearnerAssessmentService learnerAssessmentService;

    /**
     * 3. 查询课程考核卡片
     */
    @GetMapping("/courses/{courseId}/assessment")
    public Result<AssessmentCardVO> getAssessmentCard(@PathVariable Long courseId) {
        Long userId = SecurityUtils.currentUserId();
        AssessmentCardVO card = learnerAssessmentService.getAssessmentCard(courseId, userId);
        return Result.success(card);
    }

    /**
     * 4. 开始或继续考试
     */
    @PostMapping("/assessments/{assessmentId}/start")
    public Result<StartAttemptVO> startAttempt(@PathVariable Long assessmentId) {
        Long userId = SecurityUtils.currentUserId();
        StartAttemptVO vo = learnerAssessmentService.startOrResumeAttempt(assessmentId, userId);
        return Result.success(vo);
    }

    /**
     * 6. 获取本次试卷
     */
    @GetMapping("/assessment-attempts/{attemptId}")
    public Result<AttemptPaperVO> getAttemptPaper(@PathVariable Long attemptId) {
        Long userId = SecurityUtils.currentUserId();
        AttemptPaperVO vo = learnerAssessmentService.getAttemptPaper(attemptId, userId);
        return Result.success(vo);
    }

    /**
     * 7. 保存单题答案
     */
    @PutMapping("/assessment-attempts/{attemptId}/answers/{attemptQuestionId}")
    public Result<SaveAnswerResponse> saveAnswer(
            @PathVariable Long attemptId,
            @PathVariable Long attemptQuestionId,
            @RequestBody SaveAnswerRequest request
    ) {
        Long userId = SecurityUtils.currentUserId();
        SaveAnswerResponse vo = learnerAssessmentService.saveAnswer(attemptId, attemptQuestionId, userId, request);
        return Result.success(vo);
    }

    /**
     * 8. 交卷
     */
    @PostMapping("/assessment-attempts/{attemptId}/submit")
    public Result<SubmitAttemptVO> submitAttempt(@PathVariable Long attemptId) {
        Long userId = SecurityUtils.currentUserId();
        SubmitAttemptVO vo = learnerAssessmentService.submitAttempt(attemptId, userId);
        return Result.success(vo);
    }

    /**
     * 9. 查看成绩详情
     */
    @GetMapping("/assessment-attempts/{attemptId}/result")
    public Result<AttemptResultVO> getAttemptResult(@PathVariable Long attemptId) {
        Long userId = SecurityUtils.currentUserId();
        AttemptResultVO vo = learnerAssessmentService.getAttemptResult(attemptId, userId);
        return Result.success(vo);
    }
}
