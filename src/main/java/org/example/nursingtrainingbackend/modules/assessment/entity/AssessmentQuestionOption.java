package org.example.nursingtrainingbackend.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 考核题目选项
 */
@Data
@TableName("assessment_question_option")
public class AssessmentQuestionOption {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目ID */
    private Long questionId;

    /** 选项标识，如A/B或TRUE/FALSE */
    private String optionKey;

    /** 选项内容 */
    private String content;

    /** 是否正确答案：0-否 1-是 */
    private Integer isCorrect;

    /** 排序序号 */
    private Integer sortOrder;
}
