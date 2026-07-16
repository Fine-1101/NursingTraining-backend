package org.example.nursingtrainingbackend.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentAttempt;
import org.example.nursingtrainingbackend.modules.assessment.dto.LearnerResultHistoryQuery;
import org.example.nursingtrainingbackend.modules.assessment.vo.ParticipantItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ReminderItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultDetailVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultExportRowVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultItemVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.ResultSummaryVO;
import org.example.nursingtrainingbackend.modules.assessment.vo.learner.AssessmentResultHistoryItemVO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssessmentAttemptMapper extends BaseMapper<AssessmentAttempt> {

    IPage<AssessmentResultHistoryItemVO> selectLearnerResultHistory(
            Page<AssessmentResultHistoryItemVO> page,
            @Param("userId") Long userId,
            @Param("query") LearnerResultHistoryQuery query);

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

    /** 查询考核参与人员分页（共用“应参加人员”规则） */
    IPage<ParticipantItemVO> selectParticipants(
            Page<ParticipantItemVO> page,
            @Param("assessmentId") Long assessmentId,
            @Param("courseId") Long courseId,
            @Param("categoryId") Long categoryId,
            @Param("participationStatus") String participationStatus,
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId);

    /** 统计某考核的答题中学员去重数 */
    Long countInProgressLearners(@Param("assessmentId") Long assessmentId);

    /** 统计某考核的已参与学员去重数 */
    Long countDistinctParticipatedLearners(@Param("assessmentId") Long assessmentId);

    /** 查询考核提醒发送历史分页 */
    IPage<ReminderItemVO> selectReminderPage(
            Page<ReminderItemVO> page,
            @Param("assessmentId") Long assessmentId,
            @Param("keyword") String keyword,
            @Param("readStatus") String readStatus,
            @Param("batchId") String batchId,
            @Param("senderId") Long senderId,
            @Param("sentFrom") LocalDateTime sentFrom,
            @Param("sentTo") LocalDateTime sentTo);

    /** 查询某考核下未参加学员ID列表（用于提醒全部未参加人员） */
    List<Long> selectNonParticipantUserIds(
            @Param("assessmentId") Long assessmentId,
            @Param("courseId") Long courseId,
            @Param("categoryId") Long categoryId);

    /** 查询某考核下今天已提醒过的学员ID集合（幂等判断） */
    List<Long> selectTodayRemindedUserIds(
            @Param("assessmentId") Long assessmentId,
            @Param("userIds") List<Long> userIds);
}
