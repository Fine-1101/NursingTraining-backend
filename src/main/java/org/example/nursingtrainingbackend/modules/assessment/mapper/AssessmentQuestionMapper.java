package org.example.nursingtrainingbackend.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentQuestion;
import org.example.nursingtrainingbackend.modules.assessment.vo.QuestionListItemVO;

@Mapper
public interface AssessmentQuestionMapper extends BaseMapper<AssessmentQuestion> {

    IPage<QuestionListItemVO> selectQuestionPage(Page<QuestionListItemVO> page,
                                                  @Param("keyword") String keyword,
                                                  @Param("categoryId") Long categoryId,
                                                  @Param("courseId") Long courseId,
                                                  @Param("questionType") Integer questionType,
                                                  @Param("difficulty") Integer difficulty,
                                                  @Param("status") Integer status,
                                                  @Param("scope") String scope);

    /** 统计某类别+题型下可用于指定课程的题目数 */
    Long countAvailableQuestions(@Param("categoryId") Long categoryId,
                                  @Param("questionType") Integer questionType,
                                  @Param("courseId") Long courseId);
}
