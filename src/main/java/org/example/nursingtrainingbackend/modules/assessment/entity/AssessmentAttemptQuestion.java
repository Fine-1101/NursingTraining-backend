package org.example.nursingtrainingbackend.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 考核试卷题目快照（开考时随机抽题并保存完整快照；后续题库修改不影响本次试卷和成绩复核）
 */
@Data
@TableName("assessment_attempt_question")
public class AssessmentAttemptQuestion {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试记录ID */
    private Long attemptId;

    /** 来源题目ID，仅用于追溯 */
    private Long sourceQuestionId;

    /** 题型：1-单选题 2-判断题 */
    private Integer questionType;

    /** 题干快照 */
    private String stemSnapshot;

    /** 选项快照，不向答题接口返回正确标记 */
    private String optionsSnapshot;

    /** 正确答案快照 */
    private String correctOptionKey;

    /** 答案解析快照 */
    private String analysisSnapshot;

    /** 题目分值 */
    private BigDecimal score;

    /** 排序序号 */
    private Integer sortOrder;
}
