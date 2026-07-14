package org.example.nursingtrainingbackend.modules.learningreport.dto;

import java.math.BigDecimal;

/**
 * 学习报告概览数据库查询结果。
 *
 * 该对象只用于接收 LearningSnapshotMapper 的统计结果，
 * 不直接返回给前端，也不直接发送给AI。
 */
public record LearningOverviewRow(

        /**
         * 有效学习行为数量。
         */
        long validLearningEventCount,

        /**
         * 发生过学习行为的天数。
         */
        int activeDays,

        /**
         * 估算学习分钟数。
         */
        long studyMinutes,

        /**
         * 已完成课程点数量。
         */
        int completedPoints,

        /**
         * 参加并提交的考核次数。
         */
        int assessmentCount,

        /**
         * 已回答题目数量。
         */
        int answeredQuestionCount,

        /**
         * 已提交考核的平均成绩。
         *
         * 没有考试成绩时为 null。
         */
        BigDecimal averageScore
) {
}
