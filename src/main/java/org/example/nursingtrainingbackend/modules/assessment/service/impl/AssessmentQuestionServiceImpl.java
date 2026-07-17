package org.example.nursingtrainingbackend.modules.assessment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.assessment.dto.*;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentQuestion;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentQuestionCourse;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentQuestionOption;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentQuestionCourseMapper;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentQuestionMapper;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentQuestionOptionMapper;
import org.example.nursingtrainingbackend.modules.assessment.service.AssessmentQuestionService;
import org.example.nursingtrainingbackend.modules.assessment.vo.*;
import org.example.nursingtrainingbackend.modules.category.entity.Category;
import org.example.nursingtrainingbackend.modules.category.mapper.CategoryMapper;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentQuestionServiceImpl implements AssessmentQuestionService {

    private final AssessmentQuestionMapper questionMapper;
    private final AssessmentQuestionOptionMapper optionMapper;
    private final AssessmentQuestionCourseMapper questionCourseMapper;
    private final CategoryMapper categoryMapper;
    private final CourseMapper courseMapper;
    /** 分页或按条件查询题目。 */

    @Override
    public PageResult<QuestionListItemVO> listQuestions(QuestionQueryDTO query) {
        Page<QuestionListItemVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<QuestionListItemVO> result = questionMapper.selectQuestionPage(
                page,
                query.getKeyword(),
                query.getCategoryId(),
                query.getCourseId(),
                query.getQuestionType(),
                query.getDifficulty(),
                query.getStatus(),
                query.getScope()
        );

        // 解析 GROUP_CONCAT 中的 courseIds 和 courseNames
        for (QuestionListItemVO item : result.getRecords()) {
            populateScopeInfo(item);
        }

        return new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }
    /** 获取题目详情。 */

    @Override
    public QuestionDetailVO getQuestionDetail(Long questionId) {
        AssessmentQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }

        QuestionDetailVO vo = new QuestionDetailVO();
        vo.setId(question.getId());
        vo.setCategoryId(question.getCategoryId());
        vo.setQuestionType(question.getQuestionType());
        vo.setStem(question.getStem());
        vo.setAnalysis(question.getAnalysis());
        vo.setDifficulty(question.getDifficulty());
        vo.setStatus(question.getStatus());

        // 类别名称
        Category category = categoryMapper.selectById(question.getCategoryId());
        vo.setCategoryName(category != null ? category.getName() : null);

        // 选项
        List<AssessmentQuestionOption> options = optionMapper.selectList(
                Wrappers.<AssessmentQuestionOption>lambdaQuery()
                        .eq(AssessmentQuestionOption::getQuestionId, questionId)
                        .orderByAsc(AssessmentQuestionOption::getSortOrder));
        vo.setOptions(options.stream().map(o -> {
            OptionVO ov = new OptionVO();
            ov.setOptionKey(o.getOptionKey());
            ov.setContent(o.getContent());
            ov.setIsCorrect(o.getIsCorrect() != null && o.getIsCorrect() == 1);
            ov.setSortOrder(o.getSortOrder());
            return ov;
        }).toList());

        // 课程范围
        List<AssessmentQuestionCourse> qcs = questionCourseMapper.selectList(
                Wrappers.<AssessmentQuestionCourse>lambdaQuery()
                        .eq(AssessmentQuestionCourse::getQuestionId, questionId));

        if (qcs.isEmpty()) {
            vo.setScope("CATEGORY_ALL");
            vo.setCourseIds(Collections.emptyList());
            vo.setCourseNames(Collections.emptyList());
        } else {
            vo.setScope("SPECIFIED_COURSE");
            List<Long> courseIds = qcs.stream().map(AssessmentQuestionCourse::getCourseId).toList();
            vo.setCourseIds(courseIds);
            if (!courseIds.isEmpty()) {
                List<Course> courses = courseMapper.selectBatchIds(courseIds);
                Map<Long, String> courseMap = courses.stream()
                        .collect(Collectors.toMap(Course::getId, Course::getTitle, (a, b) -> a));
                vo.setCourseNames(courseIds.stream()
                        .map(id -> courseMap.getOrDefault(id, "")).toList());
            } else {
                vo.setCourseNames(Collections.emptyList());
            }
        }

        return vo;
    }
    /** 创建题目。 */

    @Override
    @Transactional
    public QuestionCreateResultVO createQuestion(QuestionSaveDTO dto) {
        validateSaveDTO(dto);

        AssessmentQuestion question = new AssessmentQuestion();
        question.setCategoryId(dto.getCategoryId());
        question.setQuestionType(dto.getQuestionType());
        question.setStem(dto.getStem());
        question.setAnalysis(dto.getAnalysis());
        question.setDifficulty(dto.getDifficulty());
        question.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        question.setCreatedBy(SecurityUtils.currentUserId());
        questionMapper.insert(question);

        saveOptions(question.getId(), dto.getOptions());
        saveCourseIds(question.getId(), dto.getCourseIds());

        QuestionCreateResultVO result = new QuestionCreateResultVO();
        result.setId(question.getId());
        return result;
    }
    /** 更新题目。 */

    @Override
    @Transactional
    public void updateQuestion(Long questionId, QuestionSaveDTO dto) {
        AssessmentQuestion existing = questionMapper.selectById(questionId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        validateSaveDTO(dto);

        existing.setCategoryId(dto.getCategoryId());
        existing.setQuestionType(dto.getQuestionType());
        existing.setStem(dto.getStem());
        existing.setAnalysis(dto.getAnalysis());
        existing.setDifficulty(dto.getDifficulty());
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        questionMapper.updateById(existing);

        // 删除旧选项并重新插入
        optionMapper.delete(Wrappers.<AssessmentQuestionOption>lambdaQuery()
                .eq(AssessmentQuestionOption::getQuestionId, questionId));
        saveOptions(questionId, dto.getOptions());

        // 删除旧课程关联并重新插入
        questionCourseMapper.delete(Wrappers.<AssessmentQuestionCourse>lambdaQuery()
                .eq(AssessmentQuestionCourse::getQuestionId, questionId));
        saveCourseIds(questionId, dto.getCourseIds());
    }
    /** 更新题目状态。 */

    @Override
    @Transactional
    public void updateQuestionStatus(Long questionId, QuestionStatusDTO dto) {
        AssessmentQuestion existing = questionMapper.selectById(questionId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        questionMapper.update(null, Wrappers.<AssessmentQuestion>lambdaUpdate()
                .eq(AssessmentQuestion::getId, questionId)
                .set(AssessmentQuestion::getStatus, dto.getStatus()));
    }
    /** 删除题目。 */

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        AssessmentQuestion existing = questionMapper.selectById(questionId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        questionMapper.deleteById(questionId);
    }

    // ==================== 私有辅助方法 ====================

    private void validateSaveDTO(QuestionSaveDTO dto) {
        // 校验题型
        if (dto.getQuestionType() != 1 && dto.getQuestionType() != 2) {
            throw new BusinessException(ErrorCode.QUESTION_TYPE_NOT_SUPPORTED);
        }

        // 校验类别存在且启用
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null || category.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 校验选项
        List<OptionDTO> options = dto.getOptions();
        if (dto.getQuestionType() == 1) {
            // 单选题至少2个选项
            if (options == null || options.size() < 2) {
                throw new BusinessException(ErrorCode.QUESTION_OPTION_INVALID, "单选题至少需要2个选项");
            }
            long correctCount = options.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .count();
            if (correctCount != 1) {
                throw new BusinessException(ErrorCode.QUESTION_OPTION_INVALID, "单选题必须有且仅有一个正确答案");
            }
        } else if (dto.getQuestionType() == 2) {
            // 判断题固定 TRUE/FALSE
            if (options == null || options.size() != 2) {
                throw new BusinessException(ErrorCode.QUESTION_OPTION_INVALID, "判断题必须恰好有2个选项");
            }
            long correctCount = options.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .count();
            if (correctCount != 1) {
                throw new BusinessException(ErrorCode.QUESTION_OPTION_INVALID, "判断题必须有且仅有一个正确答案");
            }
        }

        // 校验指定课程的类别属于题目类别或其子类别
        List<Long> courseIds = dto.getCourseIds();
        if (courseIds != null && !courseIds.isEmpty()) {
            List<Course> courses = courseMapper.selectBatchIds(courseIds);
            if (courses.size() != courseIds.size()) {
                throw new BusinessException(ErrorCode.QUESTION_COURSE_CATEGORY_MISMATCH, "部分指定课程不存在");
            }
            Set<Long> descendantIds = getDescendantCategoryIds(dto.getCategoryId());
            for (Course c : courses) {
                if (!descendantIds.contains(c.getCategoryId())) {
                    throw new BusinessException(ErrorCode.QUESTION_COURSE_CATEGORY_MISMATCH,
                            "课程(id=" + c.getId() + ")的类别不属于题目类别或其子类别");
                }
            }
        }
    }

    private void saveOptions(Long questionId, List<OptionDTO> options) {
        if (options == null || options.isEmpty()) return;
        for (int i = 0; i < options.size(); i++) {
            OptionDTO od = options.get(i);
            AssessmentQuestionOption option = new AssessmentQuestionOption();
            option.setQuestionId(questionId);
            option.setOptionKey(od.getOptionKey());
            option.setContent(od.getContent());
            option.setIsCorrect(Boolean.TRUE.equals(od.getIsCorrect()) ? 1 : 0);
            option.setSortOrder(od.getSortOrder() != null ? od.getSortOrder() : i + 1);
            optionMapper.insert(option);
        }
    }

    private void saveCourseIds(Long questionId, List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        for (Long courseId : courseIds) {
            AssessmentQuestionCourse qc = new AssessmentQuestionCourse();
            qc.setQuestionId(questionId);
            qc.setCourseId(courseId);
            qc.setCreatedAt(now);
            questionCourseMapper.insert(qc);
        }
    }

    private void populateScopeInfo(QuestionListItemVO item) {
        String idsStr = item.getCourseIdsStr();
        String namesStr = item.getCourseNamesStr();

        if (idsStr == null || idsStr.isBlank()) {
            item.setCourseIds(Collections.emptyList());
            item.setCourseNames(Collections.emptyList());
        } else {
            List<Long> ids = Arrays.stream(idsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
            item.setCourseIds(ids);

            if (namesStr != null && !namesStr.isBlank()) {
                item.setCourseNames(Arrays.stream(namesStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList());
            } else {
                item.setCourseNames(Collections.emptyList());
            }
        }
    }

    /**
     * 获取指定类别及其所有子孙类别ID集合。
     */
    private Set<Long> getDescendantCategoryIds(Long categoryId) {
        List<Category> allCategories = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().isNull(Category::getDeletedAt));
        Map<Long, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));
        Set<Long> result = new HashSet<>();
        collectDescendants(categoryId, childrenMap, result);
        return result;
    }

    private void collectDescendants(Long parentId, Map<Long, List<Category>> childrenMap, Set<Long> result) {
        result.add(parentId);
        List<Category> children = childrenMap.get(parentId);
        if (children != null) {
            for (Category child : children) {
                collectDescendants(child.getId(), childrenMap, result);
            }
        }
    }
}
