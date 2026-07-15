package org.example.nursingtrainingbackend.modules.learningreport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI接口调用日志表，记录所有大模型请求明细用于计费、排查、统计
 */
@Data
@TableName("ai_invocation_log")
public class AiInvocationLog {

    /**
     * 主键自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联AI学习报告ID，ai_learning_report.id，无则为空
     */
    private Long reportId;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 单次AI请求唯一流水号
     */
    private String requestId;

    /**
     * AI服务商名称
     */
    private String provider;

    /**
     * 调用的大模型名称
     */
    private String modelName;

    /**
     * 本次调用使用的提示词版本
     */
    private String promptVersion;

    /**
     * 调用状态：请求中/成功/失败
     */
    private String status;

    /**
     * 接口耗时，单位毫秒
     */
    private Integer latencyMs;

    /**
     * 请求入参token消耗
     */
    private Integer inputTokens;

    /**
     * 模型返回token消耗
     */
    private Integer outputTokens;

    /**
     * 接口重试次数
     */
    private Integer retryCount;

    /**
     * 调用失败错误码
     */
    private String errorCode;

    /**
     * 调用请求发起时间
     */
    private LocalDateTime createdAt;
}
