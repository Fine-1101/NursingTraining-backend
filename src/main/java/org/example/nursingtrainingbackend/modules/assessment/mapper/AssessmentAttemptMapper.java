package org.example.nursingtrainingbackend.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentAttempt;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultDetailVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultExportRowVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultSummaryVO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssessmentAttemptMapper extends BaseMapper<AssessmentAttempt> {

    IPage<ResultItemVO> selectResultPage(Page<ResultItemVO> page,
                                          @Param("assessmentId") Long assessmentId,
                                          @Param("courseId") Long courseId,
                                          @Param("categoryId") Long categoryId,
                                          @Param("departmentId") Long departmentId,
                                          @Param("keyword") String keyword,
                                          @Param("passed") Boolean passed,
                                          @Param("submittedFrom") LocalDateTime submittedFrom,
                                          @Param("submittedTo") LocalDateTime submittedTo);

    ResultDetailVO selectResultDetail(@Param("attemptId") Long attemptId);

    ResultSummaryVO selectResultSummary(@Param("assessmentId") Long assessmentId);

    Long countEligibleLearners(@Param("courseId") Long courseId,
                               @Param("categoryId") Long categoryId);

    List<ResultExportRowVO> selectResultExport(@Param("assessmentId") Long assessmentId,
                                                @Param("courseId") Long courseId,
                                                @Param("categoryId") Long categoryId,
                                                @Param("departmentId") Long departmentId,
                                                @Param("keyword") String keyword,
                                                @Param("passed") Boolean passed,
                                                @Param("submittedFrom") LocalDateTime submittedFrom,
                                                @Param("submittedTo") LocalDateTime submittedTo);
}
