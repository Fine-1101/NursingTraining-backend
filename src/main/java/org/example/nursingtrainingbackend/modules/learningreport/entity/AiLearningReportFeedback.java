package org.example.nursingtrainingbackend.modules.learningreport.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI学习报告用户反馈表，收集用户对报告的评价与改进意见
 */
@Data
@TableName("ai_learning_report_feedback")
public class AiLearningReportFeedback {

    /**
     * 主键自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 反馈所属报告ID，关联ai_learning_report.id
     */
    private Long reportId;

    /**
     * 提交反馈的用户ID
     */
    private Long userId;

    /**
     * 报告评价：0无用 1有用
     */
    private Integer helpful;

    /**
     * 反馈标签编码集合JSON，如["内容不全","数据错误"]
     */
    private String reasonCodes;

    /**
     * 用户自定义文字反馈
     */
    private String comment;

    /**
     * 反馈提交时间
     */
    private LocalDateTime createdAt;

    /**
     * 反馈修改更新时间
     */
    private LocalDateTime updatedAt;
}