package org.example.nursingtrainingbackend.modules.assessment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.assessment.dto.ResultQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.entity.Assessment;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentAnswer;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentAttempt;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentAttemptQuestion;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentAnswerMapper;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentAttemptMapper;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentAttemptQuestionMapper;
import org.example.nursingtrainingbackend.modules.assessment.mapper.AssessmentMapper;
import org.example.nursingtrainingbackend.modules.assessment.service.AssessmentResultService;
import org.example.nursingtrainingbackend.modules.assessment.vo.*;
import org.example.nursingtrainingbackend.modules.user.entity.Department;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.DepartmentMapper;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentResultServiceImpl implements AssessmentResultService {

    private final AssessmentAttemptMapper attemptMapper;
    private final AssessmentAttemptQuestionMapper attemptQuestionMapper;
    private final AssessmentAnswerMapper answerMapper;
    private final AssessmentMapper assessmentMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<ResultItemVO> listResults(ResultQueryDTO query) {
        Page<ResultItemVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<ResultItemVO> result = attemptMapper.selectResultPage(
                page,
                query.getAssessmentId(),
                query.getCourseId(),
                query.getCategoryId(),
                query.getDepartmentId(),
                query.getKeyword(),
                query.getPassed(),
                query.getSubmittedFrom(),
                query.getSubmittedTo()
        );
        return new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public ResultDetailVO getResultDetail(Long attemptId) {
        AssessmentAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND, "考试记录不存在");
        }

        // 通过 mapper XML 查询基本信息
        ResultDetailVO detail = attemptMapper.selectResultDetail(attemptId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND, "考试记录不存在");
        }

        // 填充用户信息
        AssessmentAttempt attemptEntity = attemptMapper.selectById(attemptId);
        if (attemptEntity == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND, "\u8003\u8bd5\u8bb0\u5f55\u4e0d\u5b58\u5728");
        }
        User user = userMapper.selectById(attemptEntity.getUserId());
        ResultDetailVO.UserInfoVO userInfo = new ResultDetailVO.UserInfoVO();
        if (user != null) {
            userInfo.setId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setRealName(user.getRealName());
            if (user.getDeptId() != null) {
                Department dept = departmentMapper.selectById(user.getDeptId());
                userInfo.setDepartmentName(dept != null ? dept.getName() : null);
            }
        }
        detail.setUser(userInfo);

        // 查询试卷题目快照
        List<AssessmentAttemptQuestion> questions = attemptQuestionMapper.selectList(
                Wrappers.<AssessmentAttemptQuestion>lambdaQuery()
                        .eq(AssessmentAttemptQuestion::getAttemptId, attemptId)
                        .orderByAsc(AssessmentAttemptQuestion::getSortOrder));

        // 查询学员答题记录
        List<AssessmentAnswer> answers = answerMapper.selectList(
                Wrappers.<AssessmentAnswer>lambdaQuery()
                        .eq(AssessmentAnswer::getAttemptId, attemptId));
        Map<Long, AssessmentAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(AssessmentAnswer::getAttemptQuestionId, a -> a, (a, b) -> a));

        int correctCount = 0;
        int wrongCount = 0;
        int unansweredCount = 0;

        List<QuestionResultItemVO> questionItems = new ArrayList<>();
        int number = 0;
        for (AssessmentAttemptQuestion aq : questions) {
            number++;
            QuestionResultItemVO qi = new QuestionResultItemVO();
            qi.setNumber(number);
            qi.setQuestionType(aq.getQuestionType());
            qi.setStem(aq.getStemSnapshot());
            qi.setCorrectOptionKey(aq.getCorrectOptionKey());
            qi.setAnalysis(aq.getAnalysisSnapshot());
            qi.setMaxScore(aq.getScore());

            // 解析选项快照 JSON
            qi.setOptions(parseOptionsSnapshot(aq.getOptionsSnapshot()));

            // 匹配答题记录
            AssessmentAnswer answer = answerMap.get(aq.getId());
            if (answer == null || answer.getSelectedOptionKey() == null || answer.getSelectedOptionKey().isBlank()) {
                unansweredCount++;
                qi.setSelectedOptionKey(null);
                qi.setCorrect(false);
                qi.setScore(BigDecimal.ZERO);
            } else {
                qi.setSelectedOptionKey(answer.getSelectedOptionKey());
                boolean isCorrect = answer.getIsCorrect() != null && answer.getIsCorrect() == 1;
                qi.setCorrect(isCorrect);
                qi.setScore(answer.getScore() != null ? answer.getScore() : BigDecimal.ZERO);
                if (isCorrect) {
                    correctCount++;
                } else {
                    wrongCount++;
                }
            }

            questionItems.add(qi);
        }

        detail.setCorrectCount(correctCount);
        detail.setWrongCount(wrongCount);
        detail.setUnansweredCount(unansweredCount);
        detail.setQuestions(questionItems);

        return detail;
    }

    @Override
    public ResultSummaryVO getResultSummary(Long assessmentId) {
        Assessment assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }

        ResultSummaryVO summary = attemptMapper.selectResultSummary(assessmentId);
        if (summary == null) {
            summary = new ResultSummaryVO();
            summary.setAssessmentId(assessmentId);
            summary.setParticipantCount(0L);
            summary.setSubmittedCount(0L);
            summary.setPassedCount(0L);
            summary.setFailedCount(0L);
            summary.setPassRate(BigDecimal.ZERO);
            summary.setAverageScore(BigDecimal.ZERO);
            summary.setHighestScore(BigDecimal.ZERO);
            summary.setLowestScore(BigDecimal.ZERO);
        }
        summary.setAssessmentId(assessmentId);

        // 计算可参加考核的学员数
        Long eligibleCount = attemptMapper.countEligibleLearners(
                assessment.getCourseId(), assessment.getCategoryId());
        summary.setEligibleLearnerCount(eligibleCount != null ? eligibleCount : 0L);

        // 确保 passRate 精度
        if (summary.getSubmittedCount() != null && summary.getSubmittedCount() > 0
                && summary.getPassedCount() != null) {
            BigDecimal rate = BigDecimal.valueOf(summary.getPassedCount())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(summary.getSubmittedCount()), 2, RoundingMode.HALF_UP);
            summary.setPassRate(rate);
        } else {
            summary.setPassRate(BigDecimal.ZERO);
        }

        return summary;
    }

    @Override
    public void exportResults(ResultQueryDTO query, HttpServletResponse response) {
        List<ResultExportRowVO> rows = attemptMapper.selectResultExport(
                query.getAssessmentId(),
                query.getCourseId(),
                query.getCategoryId(),
                query.getDepartmentId(),
                query.getKeyword(),
                query.getPassed(),
                query.getSubmittedFrom(),
                query.getSubmittedTo()
        );

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("考核成绩");

            // 表头
            String[] headers = {"工号", "姓名", "科室", "考核名称", "课程名称",
                    "考试次数", "得分", "总分", "及格分", "是否通过",
                    "开始时间", "交卷时间", "用时(秒)"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowIdx = 1;
            for (ResultExportRowVO row : rows) {
                Row dataRow = sheet.createRow(rowIdx++);
                dataRow.createCell(0).setCellValue(row.getUsername() != null ? row.getUsername() : "");
                dataRow.createCell(1).setCellValue(row.getRealName() != null ? row.getRealName() : "");
                dataRow.createCell(2).setCellValue(row.getDepartmentName() != null ? row.getDepartmentName() : "");
                dataRow.createCell(3).setCellValue(row.getAssessmentTitle() != null ? row.getAssessmentTitle() : "");
                dataRow.createCell(4).setCellValue(row.getCourseTitle() != null ? row.getCourseTitle() : "");
                dataRow.createCell(5).setCellValue(row.getAttemptNo() != null ? row.getAttemptNo() : 0);
                dataRow.createCell(6).setCellValue(row.getScore() != null ? row.getScore().doubleValue() : 0);
                dataRow.createCell(7).setCellValue(row.getTotalScore() != null ? row.getTotalScore().doubleValue() : 0);
                dataRow.createCell(8).setCellValue(row.getPassScore() != null ? row.getPassScore().doubleValue() : 0);
                dataRow.createCell(9).setCellValue(row.getPassedText() != null ? row.getPassedText() : "");
                dataRow.createCell(10).setCellValue(row.getStartedAt() != null ? row.getStartedAt().format(DT_FMT) : "");
                dataRow.createCell(11).setCellValue(row.getSubmittedAt() != null ? row.getSubmittedAt().format(DT_FMT) : "");
                dataRow.createCell(12).setCellValue(row.getDurationSeconds() != null ? row.getDurationSeconds() : 0);
            }

            // 自动列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String filename = URLEncoder.encode("assessment-results.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }
        } catch (Exception e) {
            log.error("导出考核成绩 Excel 失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 导出失败");
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析 optionsSnapshot JSON 字符串为 SimpleOptionVO 列表。
     * 快照格式: [{"optionKey":"A","content":"..."},...]
     */
    private List<QuestionResultItemVO.SimpleOptionVO> parseOptionsSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            // 手动解析简单 JSON 数组，避免引入额外依赖
            List<QuestionResultItemVO.SimpleOptionVO> result = new ArrayList<>();
            json = json.trim();
            if (!json.startsWith("[")) return Collections.emptyList();

            List<Map<String, Object>> list = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> map : list) {
                QuestionResultItemVO.SimpleOptionVO opt = new QuestionResultItemVO.SimpleOptionVO();
                opt.setOptionKey((String) map.get("optionKey"));
                opt.setContent((String) map.get("content"));
                result.add(opt);
            }
            return result;
        } catch (Exception e) {
            log.warn("解析选项快照失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
