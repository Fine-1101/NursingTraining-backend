package org.example.nursingtrainingbackend.modules.learningreport.ai;

import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 开发和测试环境使用的假 AI 客户端。
 *
 * <p>该实现不会访问任何外部 AI 服务，只返回固定结构的报告，
 * 用于提前联调报告生成流程、数据库状态和前端页面。</p>
 */
@Component
@Profile("test-ai")
public class FakeAiReportClient implements AiReportClient {

    @Override
    public AiReportResult generate(
            LearningReportSnapshot snapshot,
            ReportGenerationOptions options
    ) {
        if (snapshot == null) {
            throw new IllegalArgumentException("学习报告快照不能为空");
        }
        if (snapshot.overview() == null) {
            throw new IllegalArgumentException("学习报告概览不能为空");
        }
        if (options == null) {
            throw new IllegalArgumentException("报告生成参数不能为空");
        }

        GeneratedLearningReport.Highlight highlight =
                new GeneratedLearningReport.Highlight(
                        "FAKE_AI_RESULT",
                        "测试环境固定结果",
                        "当前报告由 FakeAiReportClient 生成，未调用真实 AI 接口。",
                        List.of(
                                "Prompt版本：" + options.promptVersion(),
                                "快照版本：" + snapshot.snapshotVersion()
                        )
                );

        GeneratedLearningReport report = new GeneratedLearningReport(
                "测试学习报告",
                "这是固定的测试结果，用于验证报告生成和展示流程。",
                "STABLE",
                snapshot.overview(),
                List.of(highlight),
                List.of(),
                List.of(),
                List.of(),
                "继续保持学习。",
                "本报告仅用于培训学习辅助，不构成临床诊断、治疗或护理操作依据。"
        );

        return new AiReportResult(
                report,
                "fake",
                "fake-model",
                0,
                0,
                10L,
                UUID.randomUUID().toString()
        );
    }
}
