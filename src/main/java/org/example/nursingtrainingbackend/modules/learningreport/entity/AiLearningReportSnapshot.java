package org.example.nursingtrainingbackend.modules.learningreport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学习数据快照表，生成报告时备份当期用户学习原始数据
 */
@Data
@TableName("ai_learning_report_snapshot")
public class AiLearningReportSnapshot {

    /**
     * 主键自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 快照数据版本号
     */
    private String snapshotVersion;

    /**
     * 快照内容MD5/SHA哈希，防篡改、去重
     */
    private String snapshotHash;

    /**
     * 快照完整学习原始数据JSON
     */
    private String snapshotContent;

    /**
     * 快照生成时间
     */
    private LocalDateTime createdAt;
}