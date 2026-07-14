package org.example.nursingtrainingbackend.modules.assessment.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.assessment.dto.QuestionQueryDTO;
import org.example.nursingtrainingbackend.modules.assessment.dto.QuestionSaveDTO;
import org.example.nursingtrainingbackend.modules.assessment.dto.QuestionStatusDTO;
import org.example.nursingtrainingbackend.modules.assessment.vo.QuestionCreateResultVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.QuestionDetailVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.QuestionListItemVO;

public interface AssessmentQuestionService {

    PageResult<QuestionListItemVO> listQuestions(QuestionQueryDTO query);

    QuestionDetailVO getQuestionDetail(Long questionId);

    QuestionCreateResultVO createQuestion(QuestionSaveDTO dto);

    void updateQuestion(Long questionId, QuestionSaveDTO dto);

    void updateQuestionStatus(Long questionId, QuestionStatusDTO dto);

    void deleteQuestion(Long questionId);
}
