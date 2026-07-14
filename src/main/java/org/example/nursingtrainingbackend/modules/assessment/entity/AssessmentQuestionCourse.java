package org.example.nursingtrainingbackend.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目指定课程范围（没有记录表示类别下所有课程可用；存在记录时，仅表内指定课程可用）
 */
@Data
@TableName("assessment_question_course")
public class AssessmentQuestionCourse {

    /** 题目ID */
    private Long questionId;

    /** 指定可用课程ID */
    private Long courseId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
