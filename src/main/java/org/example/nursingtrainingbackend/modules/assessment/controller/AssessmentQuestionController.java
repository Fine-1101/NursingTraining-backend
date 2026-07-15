package org.example.nursingtrainingbackend.modules.assessment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.assessment.dto.QuestionQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.dto.QuestionSaveDTO;
import org.example.nursingtrainingbackend.modules.assessment.dto.QuestionStatusDTO;
import org.example.nursingtrainingbackend.modules.assessment.service.AssessmentQuestionService;
import org.example.nursingtrainingbackend.modules.assessment.vo.QuestionCreateResultVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.QuestionDetailVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.QuestionListItemVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/assessment/questions")
@RequiredArgsConstructor
public class AssessmentQuestionController {

    private final AssessmentQuestionService questionService;

    /** API-2: 查询题目列表 */
    @GetMapping
    public Result<PageResult<QuestionListItemVO>> listQuestions(@Valid QuestionQueryDTO query) {
        return Result.success(questionService.listQuestions(query));
    }

    /** API-3: 查询题目详情 */
    @GetMapping("/{questionId}")
    public Result<QuestionDetailVO> getQuestionDetail(@PathVariable Long questionId) {
        return Result.success(questionService.getQuestionDetail(questionId));
    }

    /** API-4: 新增题目 */
    @PostMapping
    public Result<QuestionCreateResultVO> createQuestion(@RequestBody @Valid QuestionSaveDTO dto) {
        return Result.success(questionService.createQuestion(dto));
    }

    /** API-5: 修改题目 */
    @PutMapping("/{questionId}")
    public Result<Void> updateQuestion(@PathVariable Long questionId,
                                       @RequestBody @Valid QuestionSaveDTO dto) {
        questionService.updateQuestion(questionId, dto);
        return Result.success();
    }

    /** API-6: 启用或停用题目 */
    @PatchMapping("/{questionId}/status")
    public Result<Void> updateQuestionStatus(@PathVariable Long questionId,
                                             @RequestBody @Valid QuestionStatusDTO dto) {
        questionService.updateQuestionStatus(questionId, dto);
        return Result.success();
    }

    /** API-7: 删除题目 */
    @DeleteMapping("/{questionId}")
    public Result<Void> deleteQuestion(@PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
        return Result.success();
    }
}
