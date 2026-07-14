package org.example.nursingtrainingbackend.modules.learningreport.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI学习报告主表
 */
@Data
@TableName("ai_learning_report")
public class AiLearningReport {

    /**
     * 主键自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，关联用户主表
     */
    private Long userId;

    /**
     * 报告类型：日/周/月/阶段报告等
     */
    private String reportType;

    /**
     * 报告生成模式：自动生成/手动触发
     */
    private String reportMode;

    /**
     * 统计周期开始时间
     */
    private LocalDateTime periodStart;

    /**
     * 统计周期结束时间
     */
    private LocalDateTime periodEnd;

    /**
     * 报告状态：待生成/生成中/成功/失败
     */
    private String status;

    /**
     * 学习阶段：入门/进阶/实战等，可为空
     */
    private String stage;

    /**
     * 报告生成进度 0-100
     */
    private Integer progress;

    /**
     * 报告标题
     */
    private String title;

    /**
     * 报告简短摘要
     */
    private String summary;

    /**
     * 完整报告结构化内容JSON字符串
     */
    private String reportContent;

    /**
     * 关联学习数据快照ID，关联ai_learning_report_snapshot.id
     */
    private Long snapshotId;

    /**
     * 快照内容哈希值，用于校验数据一致性
     */
    private String snapshotHash;

    /**
     * 是否AI生成 0否 1是
     */
    private Integer generatedByAi;

    /**
     * AI服务商：deepseek/openai/通义千问等
     */
    private String provider;

    /**
     * 使用的大模型名称
     */
    private String modelName;

    /**
     * 生成报告使用的提示词版本号
     */
    private String promptVersion;

    /**
     * 报告数据结构版本，用于兼容旧报告
     */
    private String schemaVersion;

    /**
     * AI入参消耗token数量
     */
    private Integer inputTokens;

    /**
     * AI返回内容消耗token数量
     */
    private Integer outputTokens;

    /**
     * 生成失败重试次数
     */
    private Integer retryCount;

    /**
     * 当前报告版本，用于迭代更新
     */
    private Integer reportVersion;

    /**
     * 上一版报告ID，关联本表id
     */
    private Long previousReportId;

    /**
     * 生成失败错误码
     */
    private String errorCode;

    /**
     * 生成失败详细错误信息
     */
    private String errorMessage;

    /**
     * 报告生成开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 报告生成完成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间
     */
    private LocalDateTime updatedAt;
}