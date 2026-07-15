package org.example.nursingtrainingbackend.modules.assessment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.assessment.entity.*;
import org.example.nursingtrainingbackend.modules.assessment.mapper.*;
import org.example.nursingtrainingbackend.modules.assessment.service.LearnerAssessmentService;
import org.example.nursingtrainingbackend.modules.assessment.vo.learner.*;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearnerAssessmentServiceImpl implements LearnerAssessmentService {

    private final AssessmentMapper assessmentMapper;
    private final AssessmentDrawRuleMapper drawRuleMapper;
    private final AssessmentAttemptMapper attemptMapper;
    private final AssessmentAttemptQuestionMapper attemptQuestionMapper;
    private final AssessmentAnswerMapper answerMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final AssessmentQuestionOptionMapper optionMapper;
    private final AssessmentQuestionCourseMapper questionCourseMapper;
    private final CourseMapper courseMapper;
    private final ObjectMapper objectMapper;

    // ==================== 3. 查询课程考核卡片 ====================

    @Override
    public AssessmentCardVO getAssessmentCard(Long courseId, Long userId) {
        // 查找该课程下已发布的考核
        Assessment assessment = assessmentMapper.selectOne(
                Wrappers.<Assessment>lambdaQuery()
                        .eq(Assessment::getCourseId, courseId)
                        .eq(Assessment::getStatus, 1) // 已发布
                        .isNull(Assessment::getDeletedAt)
                        .last("LIMIT 1")
        );
        if (assessment == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        // 统计该学员已使用次数（已交卷 + 超时）
        Long usedAttempts = attemptMapper.selectCount(
                Wrappers.<AssessmentAttempt>lambdaQuery()
                        .eq(AssessmentAttempt::getAssessmentId, assessment.getId())
                        .eq(AssessmentAttempt::getUserId, userId)
                        .in(AssessmentAttempt::getStatus, 2, 3) // 已交卷、已超时
        );

        // 查找进行中的 attempt
        AssessmentAttempt inProgress = attemptMapper.selectOne(
                Wrappers.<AssessmentAttempt>lambdaQuery()
                        .eq(AssessmentAttempt::getAssessmentId, assessment.getId())
                        .eq(AssessmentAttempt::getUserId, userId)
                        .eq(AssessmentAttempt::getStatus, 1) // 答题中
                        .last("LIMIT 1")
        );

        // 查找最新已完成的 attempt
        AssessmentAttempt latestFinished = attemptMapper.selectOne(
                Wrappers.<AssessmentAttempt>lambdaQuery()
                        .eq(AssessmentAttempt::getAssessmentId, assessment.getId())
                        .eq(AssessmentAttempt::getUserId, userId)
                        .in(AssessmentAttempt::getStatus, 2, 3)
                        .orderByDesc(AssessmentAttempt::getSubmittedAt)
                        .last("LIMIT 1")
        );

        int used = usedAttempts.intValue();
        int remaining = Math.max(0, assessment.getMaxAttempts() - used);

        // 计算 state
        String state;
        String action;
        boolean actionEnabled;
        String disabledReason = null;

        if (now.isBefore(assessment.getStartAt())) {
            state = "NOT_OPEN";
            action = "NONE";
            actionEnabled = false;
            disabledReason = "考核将于" + assessment.getStartAt().toLocalDate()
                    + " " + assessment.getStartAt().toLocalTime().truncatedTo(ChronoUnit.MINUTES) + "开始";
        } else if (assessment.getEndAt() != null && now.isAfter(assessment.getEndAt())) {
            state = "CLOSED";
            action = "NONE";
            actionEnabled = false;
            disabledReason = "考核已结束";
        } else if (inProgress != null) {
            state = "IN_PROGRESS";
            action = "CONTINUE";
            actionEnabled = true;
        } else if (used >= assessment.getMaxAttempts()) {
            state = "NO_ATTEMPTS";
            action = "NONE";
            actionEnabled = false;
            disabledReason = "考试次数已用完";
        } else if (latestFinished != null) {
            if (Integer.valueOf(1).equals(latestFinished.getPassed())) {
                state = "PASSED";
                action = "VIEW_RESULT";
                actionEnabled = true;
            } else {
                state = "FAILED";
                if (remaining > 0 && (assessment.getEndAt() == null || now.isBefore(assessment.getEndAt()))) {
                    action = "RETRY";
                    actionEnabled = true;
                } else {
                    action = "VIEW_RESULT";
                    actionEnabled = true;
                }
            }
        } else {
            state = "NOT_STARTED";
            action = "START";
            actionEnabled = true;
        }

        return new AssessmentCardVO(
                assessment.getId(),
                assessment.getTitle(),
                assessment.getDescription(),
                assessment.getStartAt(),
                assessment.getEndAt(),
                now,
                assessment.getDurationMinutes(),
                assessment.getTotalScore(),
                assessment.getPassScore(),
                assessment.getMaxAttempts(),
                used,
                remaining,
                state,
                inProgress != null ? inProgress.getId() : null,
                inProgress != null ? inProgress.getDeadlineAt() : null,
                latestFinished != null ? latestFinished.getId() : null,
                latestFinished != null ? latestFinished.getScore() : null,
                latestFinished != null ? (Integer.valueOf(1).equals(latestFinished.getPassed())) : null,
                action,
                actionEnabled,
                disabledReason
        );
    }

    // ==================== 4. 开始或继续考试 ====================

    @Override
    @Transactional
    public StartAttemptVO startOrResumeAttempt(Long assessmentId, Long userId) {
        Assessment assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(assessment.getStatus())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_STATUS_CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(assessment.getStartAt())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_STARTED);
        }
        if (assessment.getEndAt() != null && now.isAfter(assessment.getEndAt())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_CLOSED);
        }

        // 检查是否有进行中的 attempt
        AssessmentAttempt inProgress = attemptMapper.selectOne(
                Wrappers.<AssessmentAttempt>lambdaQuery()
                        .eq(AssessmentAttempt::getAssessmentId, assessmentId)
                        .eq(AssessmentAttempt::getUserId, userId)
                        .eq(AssessmentAttempt::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (inProgress != null) {
            // 检查是否已超时
            if (now.isAfter(inProgress.getDeadlineAt())) {
                autoSubmitAttempt(inProgress, assessment);
                throw new BusinessException(ErrorCode.ASSESSMENT_AUTO_SUBMITTED);
            }
            return new StartAttemptVO(
                    inProgress.getId(),
                    assessmentId,
                    inProgress.getAttemptNo(),
                    inProgress.getStartedAt(),
                    inProgress.getDeadlineAt(),
                    now,
                    true
            );
        }

        // 检查次数
        Long usedCount = attemptMapper.selectCount(
                Wrappers.<AssessmentAttempt>lambdaQuery()
                        .eq(AssessmentAttempt::getAssessmentId, assessmentId)
                        .eq(AssessmentAttempt::getUserId, userId)
                        .in(AssessmentAttempt::getStatus, 2, 3)
        );
        if (usedCount >= assessment.getMaxAttempts()) {
            throw new BusinessException(ErrorCode.ASSESSMENT_ATTEMPTS_EXHAUSTED);
        }

        // 创建新 attempt
        int attemptNo = usedCount.intValue() + 1;
        LocalDateTime deadlineAt = now.plusMinutes(assessment.getDurationMinutes());

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessmentId(assessmentId);
        attempt.setUserId(userId);
        attempt.setAttemptNo(attemptNo);
        attempt.setStatus(1); // 答题中
        attempt.setStartedAt(now);
        attempt.setDeadlineAt(deadlineAt);
        attemptMapper.insert(attempt);

        // 随机组卷
        drawQuestions(assessment, attempt);

        return new StartAttemptVO(
                attempt.getId(),
                assessmentId,
                attemptNo,
                now,
                deadlineAt,
                now,
                false
        );
    }

    // ==================== 5. 获取试卷详情 ====================

    @Override
    public AttemptPaperVO getAttemptPaper(Long attemptId, Long userId) {
        AssessmentAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_ATTEMPT_NOT_FOUND);
        }
        if (!attempt.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ASSESSMENT_ATTEMPT_NOT_OWNER);
        }

        // 超时自动交卷
        LocalDateTime now = LocalDateTime.now();
        if (Integer.valueOf(1).equals(attempt.getStatus()) && now.isAfter(attempt.getDeadlineAt())) {
            Assessment assessment = assessmentMapper.selectById(attempt.getAssessmentId());
            autoSubmitAttempt(attempt, assessment);
            throw new BusinessException(ErrorCode.ASSESSMENT_AUTO_SUBMITTED);
        }

        Assessment assessment = assessmentMapper.selectById(attempt.getAssessmentId());

        // 获取试卷题目
        List<AssessmentAttemptQuestion> questions = attemptQuestionMapper.selectList(
                Wrappers.<AssessmentAttemptQuestion>lambdaQuery()
                        .eq(AssessmentAttemptQuestion::getAttemptId, attemptId)
                        .orderByAsc(AssessmentAttemptQuestion::getSortOrder)
        );

        // 获取已保存的答案
        Map<Long, AssessmentAnswer> answerMap = answerMapper.selectList(
                Wrappers.<AssessmentAnswer>lambdaQuery()
                        .eq(AssessmentAnswer::getAttemptId, attemptId)
        ).stream().collect(Collectors.toMap(AssessmentAnswer::getAttemptQuestionId, a -> a));

        List<AttemptPaperVO.PaperQuestionVO> questionVOs = questions.stream().map(q -> {
            List<AttemptPaperVO.PaperOptionVO> options = parseOptionsSnapshot(q.getOptionsSnapshot());
            AssessmentAnswer answer = answerMap.get(q.getId());
            return new AttemptPaperVO.PaperQuestionVO(
                    q.getId(),
                    q.getQuestionType(),
                    q.getStemSnapshot(),
                    q.getScore(),
                    q.getSortOrder(),
                    options,
                    answer != null ? answer.getSelectedOptionKey() : null,
                    answer != null ? answer.getAnsweredAt() : null
            );
        }).toList();

        int answeredCount = (int) questionVOs.stream().filter(q -> q.selectedOptionKey() != null && !q.selectedOptionKey().isEmpty()).count();

        return new AttemptPaperVO(
                attemptId,
                attempt.getAssessmentId(),
                assessment.getTitle(),
                attempt.getAttemptNo(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getDeadlineAt(),
                now,
                assessment.getTotalScore(),
                answeredCount,
                questions.size(),
                questionVOs
        );
    }

    // ==================== 6. 保存单题答案 ====================

    @Override
    @Transactional
    public SaveAnswerResponse saveAnswer(Long attemptId, Long attemptQuestionId, Long userId, SaveAnswerRequest request) {
        AssessmentAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_ATTEMPT_NOT_FOUND);
        }
        if (!attempt.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ASSESSMENT_ATTEMPT_NOT_OWNER);
        }
        if (!Integer.valueOf(1).equals(attempt.getStatus())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_ALREADY_SUBMITTED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(attempt.getDeadlineAt())) {
            Assessment assessment = assessmentMapper.selectById(attempt.getAssessmentId());
            autoSubmitAttempt(attempt, assessment);
            throw new BusinessException(ErrorCode.ASSESSMENT_AUTO_SUBMITTED);
        }

        // 验证题目属于本试卷
        AssessmentAttemptQuestion q = attemptQuestionMapper.selectById(attemptQuestionId);
        if (q == null || !q.getAttemptId().equals(attemptId)) {
            throw new BusinessException(ErrorCode.ASSESSMENT_OPTION_MISMATCH);
        }

        // 查找已有答案记录（upsert）
        AssessmentAnswer existing = answerMapper.selectOne(
                Wrappers.<AssessmentAnswer>lambdaQuery()
                        .eq(AssessmentAnswer::getAttemptId, attemptId)
                        .eq(AssessmentAnswer::getAttemptQuestionId, attemptQuestionId)
        );

        if (existing != null) {
            existing.setSelectedOptionKey(request.selectedOptionKey());
            existing.setAnsweredAt(now);
            answerMapper.updateById(existing);
        } else {
            AssessmentAnswer answer = new AssessmentAnswer();
            answer.setAttemptId(attemptId);
            answer.setAttemptQuestionId(attemptQuestionId);
            answer.setSelectedOptionKey(request.selectedOptionKey());
            answer.setIsCorrect(0); // 交卷时统一判分
            answer.setScore(BigDecimal.ZERO);
            answer.setAnsweredAt(now);
            answerMapper.insert(answer);
        }

        long remainingSeconds = Math.max(0, java.time.Duration.between(now, attempt.getDeadlineAt()).getSeconds());

        return new SaveAnswerResponse(
                attemptQuestionId,
                request.selectedOptionKey(),
                now,
                now,
                attempt.getDeadlineAt(),
                remainingSeconds
        );
    }

    // ==================== 7. 交卷 ====================

    @Override
    @Transactional
    public SubmitAttemptVO submitAttempt(Long attemptId, Long userId) {
        AssessmentAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_ATTEMPT_NOT_FOUND);
        }
        if (!attempt.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ASSESSMENT_ATTEMPT_NOT_OWNER);
        }

        Assessment assessment = assessmentMapper.selectById(attempt.getAssessmentId());

        // 重复提交：如果已经交卷，直接返回已有成绩
        if (!Integer.valueOf(1).equals(attempt.getStatus())) {
            return buildSubmitVO(attempt, assessment, userId);
        }

        // 判分
        gradeAttempt(attempt, assessment);

        return buildSubmitVO(attempt, assessment, userId);
    }

    // ==================== 8. 查看成绩 ====================

    @Override
    public AttemptResultVO getAttemptResult(Long attemptId, Long userId) {
        AssessmentAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_ATTEMPT_NOT_FOUND);
        }
        if (!attempt.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ASSESSMENT_ATTEMPT_NOT_OWNER);
        }
        if (Integer.valueOf(1).equals(attempt.getStatus())) {
            // 还在答题中，检查超时
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(attempt.getDeadlineAt())) {
                Assessment assessment = assessmentMapper.selectById(attempt.getAssessmentId());
                autoSubmitAttempt(attempt, assessment);
            } else {
                throw new BusinessException(ErrorCode.ASSESSMENT_STATUS_CONFLICT, "考试尚未交卷");
            }
        }

        Assessment assessment = assessmentMapper.selectById(attempt.getAssessmentId());
        Course course = courseMapper.selectById(assessment.getCourseId());

        // 统计答题情况
        int[] stats = calculateStats(attemptId);
        int correctCount = stats[0];
        int wrongCount = stats[1];
        int unansweredCount = stats[2];

        long durationSeconds = 0;
        if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null) {
            durationSeconds = java.time.Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();
        }

        int remainingAttempts = calculateRemainingAttempts(assessment, userId);

        return new AttemptResultVO(
                attemptId,
                attempt.getAssessmentId(),
                assessment.getTitle(),
                course != null ? course.getId() : null,
                course != null ? course.getTitle() : null,
                attempt.getAttemptNo(),
                attempt.getScore(),
                assessment.getTotalScore(),
                assessment.getPassScore(),
                Integer.valueOf(1).equals(attempt.getPassed()),
                correctCount,
                wrongCount,
                unansweredCount,
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                durationSeconds,
                attempt.getStatus(),
                remainingAttempts
        );
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 随机组卷：按抽题规则从题库中随机抽取题目并保存快照
     */
    private void drawQuestions(Assessment assessment, AssessmentAttempt attempt) {
        List<AssessmentDrawRule> rules = drawRuleMapper.selectList(
                Wrappers.<AssessmentDrawRule>lambdaQuery()
                        .eq(AssessmentDrawRule::getAssessmentId, assessment.getId())
        );

        int sortOrder = 0;
        for (AssessmentDrawRule rule : rules) {
            if (rule.getQuestionCount() == null || rule.getQuestionCount() <= 0) {
                continue;
            }

            // 查询可用题目
            List<AssessmentQuestion> candidates = questionMapper.selectList(
                    Wrappers.<AssessmentQuestion>lambdaQuery()
                            .eq(AssessmentQuestion::getCategoryId, assessment.getCategoryId())
                            .eq(AssessmentQuestion::getQuestionType, rule.getQuestionType())
                            .eq(AssessmentQuestion::getStatus, 1)
                            .isNull(AssessmentQuestion::getDeletedAt)
            );

            // 过滤课程范围
            candidates = filterByCourseScope(candidates, assessment.getCourseId());

            if (candidates.size() < rule.getQuestionCount()) {
                throw new BusinessException(ErrorCode.ASSESSMENT_DRAW_FAILED,
                        "题型" + rule.getQuestionType() + "可用题目不足");
            }

            // 随机抽取
            Collections.shuffle(candidates);
            List<AssessmentQuestion> drawn = candidates.subList(0, rule.getQuestionCount());

            for (AssessmentQuestion question : drawn) {
                sortOrder++;

                // 获取选项
                List<AssessmentQuestionOption> options = optionMapper.selectList(
                        Wrappers.<AssessmentQuestionOption>lambdaQuery()
                                .eq(AssessmentQuestionOption::getQuestionId, question.getId())
                                .orderByAsc(AssessmentQuestionOption::getSortOrder)
                );

                // 找正确答案
                String correctKey = options.stream()
                        .filter(o -> Integer.valueOf(1).equals(o.getIsCorrect()))
                        .map(AssessmentQuestionOption::getOptionKey)
                        .findFirst()
                        .orElse("");

                // 序列化选项快照
                String optionsJson;
                try {
                    List<Map<String, String>> optionList = options.stream().map(o -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("optionKey", o.getOptionKey());
                        m.put("content", o.getContent());
                        return m;
                    }).toList();
                    optionsJson = objectMapper.writeValueAsString(optionList);
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.ASSESSMENT_DRAW_FAILED, "选项序列化失败");
                }

                AssessmentAttemptQuestion aq = new AssessmentAttemptQuestion();
                aq.setAttemptId(attempt.getId());
                aq.setSourceQuestionId(question.getId());
                aq.setQuestionType(question.getQuestionType());
                aq.setStemSnapshot(question.getStem());
                aq.setOptionsSnapshot(optionsJson);
                aq.setCorrectOptionKey(correctKey);
                aq.setAnalysisSnapshot(question.getAnalysis());
                aq.setScore(rule.getScorePerQuestion());
                aq.setSortOrder(sortOrder);
                attemptQuestionMapper.insert(aq);
            }
        }
    }

    /**
     * 根据题目-课程范围过滤
     */
    private List<AssessmentQuestion> filterByCourseScope(List<AssessmentQuestion> candidates, Long courseId) {
        if (candidates.isEmpty()) return candidates;

        List<Long> candidateIds = candidates.stream().map(AssessmentQuestion::getId).toList();
        List<AssessmentQuestionCourse> courseBindings = questionCourseMapper.selectList(
                Wrappers.<AssessmentQuestionCourse>lambdaQuery()
                        .in(AssessmentQuestionCourse::getQuestionId, candidateIds)
        );

        if (courseBindings.isEmpty()) {
            return candidates; // 没有课程绑定，全部可用
        }

        Map<Long, Set<Long>> questionCourseMap = new HashMap<>();
        for (AssessmentQuestionCourse qc : courseBindings) {
            questionCourseMap.computeIfAbsent(qc.getQuestionId(), k -> new HashSet<>()).add(qc.getCourseId());
        }

        return candidates.stream().filter(q -> {
            Set<Long> boundCourses = questionCourseMap.get(q.getId());
            return boundCourses == null || boundCourses.contains(courseId);
        }).collect(Collectors.toList());
    }

    /**
     * 自动交卷（超时）
     */
    private void autoSubmitAttempt(AssessmentAttempt attempt, Assessment assessment) {
        if (attempt == null || !Integer.valueOf(1).equals(attempt.getStatus())) return;
        gradeAttempt(attempt, assessment);
    }

    /**
     * 判分：逐题对比正确答案并计算总分
     */
    private void gradeAttempt(AssessmentAttempt attempt, Assessment assessment) {
        List<AssessmentAttemptQuestion> questions = attemptQuestionMapper.selectList(
                Wrappers.<AssessmentAttemptQuestion>lambdaQuery()
                        .eq(AssessmentAttemptQuestion::getAttemptId, attempt.getId())
        );

        Map<Long, AssessmentAnswer> answerMap = answerMapper.selectList(
                Wrappers.<AssessmentAnswer>lambdaQuery()
                        .eq(AssessmentAnswer::getAttemptId, attempt.getId())
        ).stream().collect(Collectors.toMap(AssessmentAnswer::getAttemptQuestionId, a -> a));

        BigDecimal totalScore = BigDecimal.ZERO;
        for (AssessmentAttemptQuestion q : questions) {
            AssessmentAnswer answer = answerMap.get(q.getId());
            String selectedKey = answer != null ? answer.getSelectedOptionKey() : null;
            boolean correct = selectedKey != null && !selectedKey.isEmpty()
                    && q.getCorrectOptionKey().equals(selectedKey);

            if (answer != null) {
                answer.setIsCorrect(correct ? 1 : 0);
                answer.setScore(correct ? q.getScore() : BigDecimal.ZERO);
                answerMapper.updateById(answer);
            }

            if (correct) {
                totalScore = totalScore.add(q.getScore());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        attempt.setStatus(now.isAfter(attempt.getDeadlineAt()) ? 3 : 2); // 超时=3，正常=2
        attempt.setScore(totalScore);
        attempt.setPassed(totalScore.compareTo(assessment.getPassScore()) >= 0 ? 1 : 0);
        attempt.setSubmittedAt(now);
        attemptMapper.updateById(attempt);
    }

    /**
     * 构建交卷响应 VO
     */
    private SubmitAttemptVO buildSubmitVO(AssessmentAttempt attempt, Assessment assessment, Long userId) {
        int[] stats = calculateStats(attempt.getId());
        int remainingAttempts = calculateRemainingAttempts(assessment, userId);

        return new SubmitAttemptVO(
                attempt.getId(),
                attempt.getAssessmentId(),
                attempt.getScore(),
                assessment.getTotalScore(),
                assessment.getPassScore(),
                Integer.valueOf(1).equals(attempt.getPassed()),
                stats[0],
                stats[1],
                stats[2],
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getStatus(),
                remainingAttempts
        );
    }

    /**
     * 统计答题情况：[correctCount, wrongCount, unansweredCount]
     */
    private int[] calculateStats(Long attemptId) {
        List<AssessmentAttemptQuestion> questions = attemptQuestionMapper.selectList(
                Wrappers.<AssessmentAttemptQuestion>lambdaQuery()
                        .eq(AssessmentAttemptQuestion::getAttemptId, attemptId)
        );
        Map<Long, AssessmentAnswer> answerMap = answerMapper.selectList(
                Wrappers.<AssessmentAnswer>lambdaQuery()
                        .eq(AssessmentAnswer::getAttemptId, attemptId)
        ).stream().collect(Collectors.toMap(AssessmentAnswer::getAttemptQuestionId, a -> a));

        int correctCount = 0;
        int wrongCount = 0;
        int unansweredCount = 0;
        for (AssessmentAttemptQuestion q : questions) {
            AssessmentAnswer answer = answerMap.get(q.getId());
            String selectedKey = answer != null ? answer.getSelectedOptionKey() : null;
            if (selectedKey == null || selectedKey.isEmpty()) {
                unansweredCount++;
            } else if (q.getCorrectOptionKey().equals(selectedKey)) {
                correctCount++;
            } else {
                wrongCount++;
            }
        }
        return new int[]{correctCount, wrongCount, unansweredCount};
    }

    /**
     * 计算剩余考试次数
     */
    private int calculateRemainingAttempts(Assessment assessment, Long userId) {
        Long usedAttempts = attemptMapper.selectCount(
                Wrappers.<AssessmentAttempt>lambdaQuery()
                        .eq(AssessmentAttempt::getAssessmentId, assessment.getId())
                        .eq(AssessmentAttempt::getUserId, userId)
                        .in(AssessmentAttempt::getStatus, 2, 3)
        );
        return Math.max(0, assessment.getMaxAttempts() - usedAttempts.intValue());
    }

    /**
     * 解析选项快照（不含正确答案标记）
     */
    private List<AttemptPaperVO.PaperOptionVO> parseOptionsSnapshot(String json) {
        if (json == null || json.isEmpty()) return List.of();
        try {
            List<Map<String, String>> list = objectMapper.readValue(json, new TypeReference<>() {});
            return list.stream()
                    .map(m -> new AttemptPaperVO.PaperOptionVO(m.get("optionKey"), m.get("content")))
                    .toList();
        } catch (Exception e) {
            log.error("解析选项快照失败: {}", json, e);
            return List.of();
        }
    }
}
