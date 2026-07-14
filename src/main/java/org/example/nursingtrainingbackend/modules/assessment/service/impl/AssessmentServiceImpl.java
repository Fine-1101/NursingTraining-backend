package org.example.nursingtrainingbackend.modules.assessment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.assessment.dto.AssessmentCreateRequest;
import org.example.nursingtrainingbackend.modules.assessment.dto.AssessmentListQuery;
import org.example.nursingtrainingbackend.modules.assessment.entity.Assessment;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentAttempt;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentDrawRule;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentAttemptMapper;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentDrawRuleMapper;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentMapper;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentQuestionMapper;
import org.example.nursingtrainingbackend.modules.assessment.service.AssessmentService;
import org.example.nursingtrainingbackend.modules.assessment.vo.*;
import org.example.nursingtrainingbackend.modules.category.entity.Category;
import org.example.nursingtrainingbackend.modules.category.mapper.CategoryMapper;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentMapper assessmentMapper;
    private final AssessmentDrawRuleMapper drawRuleMapper;
    private final AssessmentAttemptMapper attemptMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final CourseMapper courseMapper;
    private final CategoryMapper categoryMapper;

    // ==================== 8. 查询考核列表 ====================

    @Override
    public PageResult<AssessmentListItemVO> listAssessments(AssessmentListQuery query) {
        long page = query.page() != null && query.page() > 0 ? query.page() : 1;
        long size = query.size() != null && query.size() > 0 ? query.size() : 10;

        Page<AssessmentListItemVO> pageParam = new Page<>(page, size);
        IPage<AssessmentListItemVO> result = assessmentMapper.selectAssessmentPage(
                pageParam,
                query.keyword(),
                query.courseId(),
                query.categoryId(),
                query.status(),
                query.startFrom(),
                query.startTo()
        );
        return new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }

    // ==================== 9. 创建考核草稿 ====================

    @Override
    @Transactional
    public AssessmentCreateResponseVO createAssessment(AssessmentCreateRequest request) {
        validateDrawRules(request.drawRules());
        validateTime(request.startAt(), request.endAt());
        validatePassScore(request.passScore());

        Course course = courseMapper.selectById(request.courseId());
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        BigDecimal totalScore = calculateTotalScore(request.drawRules());

        Assessment assessment = new Assessment();
        assessment.setCourseId(request.courseId());
        assessment.setCategoryId(course.getCategoryId());
        assessment.setTitle(request.title());
        assessment.setDescription(request.description());
        assessment.setStartAt(request.startAt());
        assessment.setEndAt(request.endAt());
        assessment.setDurationMinutes(request.durationMinutes());
        assessment.setTotalScore(totalScore);
        assessment.setPassScore(request.passScore());
        assessment.setMaxAttempts(request.maxAttempts());
        assessment.setStatus(0); // 草稿
        assessment.setCreatedBy(SecurityUtils.currentUserId());
        assessmentMapper.insert(assessment);

        insertDrawRules(assessment.getId(), request.drawRules());

        return new AssessmentCreateResponseVO(
                assessment.getId(),
                assessment.getCategoryId(),
                totalScore,
                assessment.getStatus()
        );
    }

    // ==================== 10. 查询考核详情 ====================

    @Override
    public AssessmentDetailVO getAssessmentDetail(Long assessmentId) {
        Assessment assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }

        // 课程名称
        Course course = courseMapper.selectById(assessment.getCourseId());
        String courseTitle = course != null ? course.getTitle() : "";

        // 类别名称
        String categoryName = "";
        if (assessment.getCategoryId() != null) {
            Category category = categoryMapper.selectById(assessment.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }

        // 抽题规则（含可用题量统计）
        List<AssessmentDrawRule> rules = drawRuleMapper.selectList(
                Wrappers.<AssessmentDrawRule>lambdaQuery()
                        .eq(AssessmentDrawRule::getAssessmentId, assessmentId)
        );

        List<AssessmentDetailVO.DrawRuleVO> drawRuleVOs = rules.stream().map(rule -> {
            Long availableCount = questionMapper.countAvailableQuestions(
                    assessment.getCategoryId(),
                    rule.getQuestionType(),
                    assessment.getCourseId()
            );
            long available = availableCount != null ? availableCount : 0L;
            boolean sufficient = available >= rule.getQuestionCount();
            return new AssessmentDetailVO.DrawRuleVO(
                    rule.getQuestionType(),
                    rule.getQuestionCount(),
                    rule.getScorePerQuestion(),
                    available,
                    sufficient
            );
        }).toList();

        return new AssessmentDetailVO(
                assessment.getId(),
                assessment.getCourseId(),
                courseTitle,
                assessment.getCategoryId(),
                categoryName,
                assessment.getTitle(),
                assessment.getDescription(),
                assessment.getStartAt(),
                assessment.getEndAt(),
                assessment.getDurationMinutes(),
                assessment.getTotalScore(),
                assessment.getPassScore(),
                assessment.getMaxAttempts(),
                assessment.getStatus(),
                drawRuleVOs
        );
    }

    // ==================== 11. 修改考核草稿 ====================

    @Override
    @Transactional
    public void updateAssessment(Long assessmentId, AssessmentCreateRequest request) {
        Assessment assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(assessment.getStatus())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_STATUS_CONFLICT);
        }

        validateDrawRules(request.drawRules());
        validateTime(request.startAt(), request.endAt());
        validatePassScore(request.passScore());

        Course course = courseMapper.selectById(request.courseId());
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        BigDecimal totalScore = calculateTotalScore(request.drawRules());

        assessment.setCourseId(request.courseId());
        assessment.setCategoryId(course.getCategoryId());
        assessment.setTitle(request.title());
        assessment.setDescription(request.description());
        assessment.setStartAt(request.startAt());
        assessment.setEndAt(request.endAt());
        assessment.setDurationMinutes(request.durationMinutes());
        assessment.setTotalScore(totalScore);
        assessment.setPassScore(request.passScore());
        assessment.setMaxAttempts(request.maxAttempts());
        assessmentMapper.updateById(assessment);

        // 删除旧规则并重新插入
        drawRuleMapper.delete(
                Wrappers.<AssessmentDrawRule>lambdaQuery()
                        .eq(AssessmentDrawRule::getAssessmentId, assessmentId)
        );
        insertDrawRules(assessmentId, request.drawRules());
    }

    // ==================== 12. 发布前检查题量 ====================

    @Override
    public QuestionCapacityVO checkQuestionCapacity(Long assessmentId) {
        Assessment assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }

        List<AssessmentDrawRule> rules = drawRuleMapper.selectList(
                Wrappers.<AssessmentDrawRule>lambdaQuery()
                        .eq(AssessmentDrawRule::getAssessmentId, assessmentId)
        );

        boolean publishable = true;
        List<QuestionCapacityVO.ItemVO> items = rules.stream().map(rule -> {
            Long available = questionMapper.countAvailableQuestions(
                    assessment.getCategoryId(),
                    rule.getQuestionType(),
                    assessment.getCourseId()
            );
            long availableCount = available != null ? available : 0L;
            boolean sufficient = availableCount >= rule.getQuestionCount();
            if (!sufficient) {
                // 标记不可发布，但不中断遍历以收集所有规则结果
            }
            return new QuestionCapacityVO.ItemVO(
                    rule.getQuestionType(),
                    rule.getQuestionCount(),
                    availableCount,
                    sufficient
            );
        }).toList();

        publishable = items.stream().allMatch(QuestionCapacityVO.ItemVO::sufficient);

        return new QuestionCapacityVO(
                assessment.getId(),
                assessment.getCourseId(),
                assessment.getCategoryId(),
                items,
                publishable
        );
    }

    // ==================== 13. 发布考核 ====================

    @Override
    @Transactional
    public PublishAssessmentVO publishAssessment(Long assessmentId) {
        Assessment assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(assessment.getStatus())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_STATUS_CONFLICT);
        }

        // 校验时间
        validateTime(assessment.getStartAt(), assessment.getEndAt());

        // 校验抽题规则与题量
        List<AssessmentDrawRule> drawRules = drawRuleMapper.selectList(
                Wrappers.<AssessmentDrawRule>lambdaQuery()
                        .eq(AssessmentDrawRule::getAssessmentId, assessmentId)
        );
        if (drawRules.isEmpty()) {
            throw new BusinessException(ErrorCode.ASSESSMENT_DRAW_RULE_INVALID);
        }

        for (AssessmentDrawRule rule : drawRules) {
            Long availableCount = questionMapper.countAvailableQuestions(
                    assessment.getCategoryId(),
                    rule.getQuestionType(),
                    assessment.getCourseId()
            );
            long available = availableCount != null ? availableCount : 0L;
            if (available < rule.getQuestionCount()) {
                String typeName = rule.getQuestionType() == 1 ? "单选题" : "判断题";
                throw new BusinessException(ErrorCode.ASSESSMENT_QUESTION_INSUFFICIENT,
                        typeName + "需要" + rule.getQuestionCount() + "道，当前可用" + available + "道");
            }
        }

        // 校验分值一致性
        BigDecimal calculatedTotal = calculateTotalScoreFromEntity(drawRules);
        if (calculatedTotal.compareTo(assessment.getTotalScore()) != 0) {
            throw new BusinessException(ErrorCode.ASSESSMENT_SCORE_INVALID);
        }
        if (assessment.getPassScore().compareTo(assessment.getTotalScore()) > 0) {
            throw new BusinessException(ErrorCode.ASSESSMENT_SCORE_INVALID);
        }

        // 发布
        LocalDateTime now = LocalDateTime.now();
        assessment.setStatus(1);
        assessment.setPublishedAt(now);
        assessmentMapper.updateById(assessment);

        return new PublishAssessmentVO(assessment.getId(), assessment.getStatus(), now);
    }

    // ==================== 14. 关闭考核 ====================

    @Override
    @Transactional
    public CloseAssessmentVO closeAssessment(Long assessmentId) {
        Assessment assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(assessment.getStatus())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_STATUS_CONFLICT);
        }

        assessment.setStatus(2);
        assessmentMapper.updateById(assessment);

        return new CloseAssessmentVO(assessment.getId(), assessment.getStatus());
    }

    // ==================== 15. 删除考核草稿 ====================

    @Override
    @Transactional
    public void deleteAssessment(Long assessmentId) {
        Assessment assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(assessment.getStatus())) {
            throw new BusinessException(ErrorCode.ASSESSMENT_STATUS_CONFLICT);
        }

        // 有考试记录不允许删除
        Long attemptCount = attemptMapper.selectCount(
                Wrappers.<AssessmentAttempt>lambdaQuery()
                        .eq(AssessmentAttempt::getAssessmentId, assessmentId)
        );
        if (attemptCount > 0) {
            throw new BusinessException(ErrorCode.ASSESSMENT_STATUS_CONFLICT, "该考核已有学员考试记录，不能删除");
        }

        // 删除抽题规则
        drawRuleMapper.delete(
                Wrappers.<AssessmentDrawRule>lambdaQuery()
                        .eq(AssessmentDrawRule::getAssessmentId, assessmentId)
        );
        // 软删除考核
        assessmentMapper.deleteById(assessmentId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验抽题规则
     */
    private void validateDrawRules(List<AssessmentCreateRequest.DrawRuleItem> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new BusinessException(ErrorCode.ASSESSMENT_DRAW_RULE_INVALID, "至少需要一条抽题规则");
        }

        boolean hasPositiveCount = false;
        for (AssessmentCreateRequest.DrawRuleItem rule : rules) {
            if (rule.questionType() != 1 && rule.questionType() != 2) {
                throw new BusinessException(ErrorCode.ASSESSMENT_DRAW_RULE_INVALID, "题型只支持单选题(1)和判断题(2)");
            }
            if (rule.questionCount() != null && rule.questionCount() > 0) {
                hasPositiveCount = true;
            }
            if (rule.scorePerQuestion() == null || rule.scorePerQuestion().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.ASSESSMENT_DRAW_RULE_INVALID, "每题分值不能为负数");
            }
        }
        if (!hasPositiveCount) {
            throw new BusinessException(ErrorCode.ASSESSMENT_DRAW_RULE_INVALID, "至少一种题型的抽题数量需大于0");
        }
    }

    /**
     * 校验时间设置
     */
    private void validateTime(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_TIME_INVALID, "开考时间不能为空");
        }
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new BusinessException(ErrorCode.ASSESSMENT_TIME_INVALID, "结束时间不能早于开考时间");
        }
    }

    /**
     * 校验及格分
     */
    private void validatePassScore(BigDecimal passScore) {
        if (passScore == null || passScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.ASSESSMENT_SCORE_INVALID, "及格分不能为负数");
        }
    }

    /**
     * 从 DTO 规则列表计算总分
     */
    private BigDecimal calculateTotalScore(List<AssessmentCreateRequest.DrawRuleItem> rules) {
        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentCreateRequest.DrawRuleItem rule : rules) {
            if (rule.questionCount() != null && rule.scorePerQuestion() != null) {
                total = total.add(rule.scorePerQuestion()
                        .multiply(BigDecimal.valueOf(rule.questionCount())));
            }
        }
        return total;
    }

    /**
     * 从实体规则列表计算总分（用于发布校验）
     */
    private BigDecimal calculateTotalScoreFromEntity(List<AssessmentDrawRule> rules) {
        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentDrawRule rule : rules) {
            if (rule.getQuestionCount() != null && rule.getScorePerQuestion() != null) {
                total = total.add(rule.getScorePerQuestion()
                        .multiply(BigDecimal.valueOf(rule.getQuestionCount())));
            }
        }
        return total;
    }

    /**
     * 批量插入抽题规则
     */
    private void insertDrawRules(Long assessmentId, List<AssessmentCreateRequest.DrawRuleItem> rules) {
        if (rules == null || rules.isEmpty()) return;
        for (AssessmentCreateRequest.DrawRuleItem ruleItem : rules) {
            AssessmentDrawRule rule = new AssessmentDrawRule();
            rule.setAssessmentId(assessmentId);
            rule.setQuestionType(ruleItem.questionType());
            rule.setQuestionCount(ruleItem.questionCount());
            rule.setScorePerQuestion(ruleItem.scorePerQuestion());
            drawRuleMapper.insert(rule);
        }
    }
}
