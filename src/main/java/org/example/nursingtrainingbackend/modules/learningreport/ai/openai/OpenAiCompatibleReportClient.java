package org.example.nursingtrainingbackend.modules.learningreport.ai.openai;

import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.config.properties.LearningReportProperties;
import org.example.nursingtrainingbackend.modules.learningreport.ai.AiReportClient;
import org.example.nursingtrainingbackend.modules.learningreport.ai.AiReportResult;
import org.example.nursingtrainingbackend.modules.learningreport.ai.ReportGenerationOptions;
import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.validator.AiReportResponseValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI Chat Completions 兼容接口实现。
 */
@Component
@Profile("!test-ai")
public class OpenAiCompatibleReportClient implements AiReportClient {

    private static final String SYSTEM_PROMPT = """
            你是一名护理培训学习分析助手。

            请根据后端提供的结构化学习数据生成个性化学习报告。

            必须遵守：
            1. 只能使用输入数据中的事实。
            2. 不得编造成绩、时长、课程或知识点。
            3. 不得重新计算后端提供的统计数字。
            4. 推荐课程只能从candidateCourses中选择。
            5. 课程、章节和知识点ID必须原样使用。
            6. 数据不足时必须明确说明。
            7. 不提供诊断、处方、治疗或临床操作建议。
            8. 只返回一个合法JSON对象。
            9. 禁止使用Markdown代码块。
            10. 禁止输出```json或```。
            11. 禁止在JSON前后输出任何解释文字。
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final LearningReportProperties properties;
    private final AiReportResponseValidator responseValidator;

    public OpenAiCompatibleReportClient(
            @Autowired(required = false)
            @Qualifier("aiReportRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            LearningReportProperties properties,
            AiReportResponseValidator responseValidator
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.responseValidator = responseValidator;
    }

    @Override
    public AiReportResult generate(
            LearningReportSnapshot snapshot,
            ReportGenerationOptions options
    ) {
        validateArguments(snapshot, options);
        long startedAt = System.nanoTime();

        String snapshotJson = serializeSnapshot(snapshot);
        String userPrompt = buildUserPrompt(snapshotJson, options.schemaVersion());

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.ai().model(),
                List.of(
                        ChatMessage.system(SYSTEM_PROMPT),
                        ChatMessage.user(userPrompt)
                ),
                options.temperature(),
                options.maxOutputTokens()
        );

        ChatCompletionResponse response = requestAiWithRetry(request);
        String content = extractContent(response);
        GeneratedLearningReport parsedReport = parseReport(content);
        GeneratedLearningReport normalizedReport = replaceOverview(parsedReport, snapshot);

        responseValidator.validate(normalizedReport, snapshot);

        return new AiReportResult(
                normalizedReport,
                properties.ai().provider(),
                properties.ai().model(),
                extractInputTokens(response),
                extractOutputTokens(response),
                elapsedMillis(startedAt),
                resolveRequestId(response)
        );
    }

    private String serializeSnapshot(LearningReportSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    ErrorCode.AI_RESPONSE_INVALID,
                    "学习快照JSON序列化失败",
                    exception
            );
        }
    }

    /**
     * 初始请求加最多 maxRetries 次重试，绝不无限重试。
     */
    private ChatCompletionResponse requestAiWithRetry(
            ChatCompletionRequest request
    ) {
        int maxRetries = properties.ai().maxRetries();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return requestAi(request);
            } catch (RestClientResponseException exception) {
                int statusCode = exception.getStatusCode().value();
                boolean retryable = statusCode == 429 || statusCode >= 500;

                if (!retryable || attempt == maxRetries) {
                    throw convertHttpException(exception);
                }

                sleepBeforeRetry(attempt + 1);
            } catch (ResourceAccessException exception) {
                if (!isConnectionOrTimeout(exception) || attempt == maxRetries) {
                    throw new BusinessException(
                            isConnectionOrTimeout(exception)
                                    ? ErrorCode.AI_PROVIDER_TIMEOUT
                                    : ErrorCode.AI_PROVIDER_UNAVAILABLE,
                            isConnectionOrTimeout(exception)
                                    ? "AI接口请求超时，已达到最大重试次数"
                                    : "AI服务暂时无法连接",
                            exception
                    );
                }

                sleepBeforeRetry(attempt + 1);
            } catch (RestClientException exception) {
                throw new BusinessException(
                        ErrorCode.AI_PROVIDER_UNAVAILABLE,
                        "AI服务调用失败",
                        exception
                );
            }
        }

        throw new BusinessException(
                ErrorCode.AI_PROVIDER_UNAVAILABLE,
                "AI服务调用失败"
        );
    }

    private ChatCompletionResponse requestAi(ChatCompletionRequest request) {
        return restClient.post()
                .uri(properties.ai().chatPath())
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);
    }

    private String extractContent(ChatCompletionResponse response) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()
                || response.choices().getFirst() == null
                || response.choices().getFirst().message() == null
                || response.choices().getFirst().message().content() == null
                || response.choices().getFirst().message().content().isBlank()) {
            throw new BusinessException(
                    ErrorCode.AI_RESPONSE_INVALID,
                    "AI接口没有返回有效报告内容"
            );
        }

        return response.choices().getFirst().message().content();
    }

    /**
     * 先清理 Markdown；解析失败时只做一次本地 JSON 边界修复。
     */
    private GeneratedLearningReport parseReport(String content) {
        String cleaned = removeMarkdownFence(content);

        try {
            return objectMapper.readValue(cleaned, GeneratedLearningReport.class);
        } catch (RuntimeException firstException) {
            String repaired = extractJsonObject(cleaned);

            if (repaired.equals(cleaned)) {
                throw new BusinessException(
                        ErrorCode.AI_RESPONSE_INVALID,
                        "AI返回的学习报告不是合法JSON",
                        firstException
                );
            }

            try {
                return objectMapper.readValue(repaired, GeneratedLearningReport.class);
            } catch (RuntimeException secondException) {
                throw new BusinessException(
                        ErrorCode.AI_RESPONSE_INVALID,
                        "AI返回的学习报告JSON修复失败",
                        secondException
                );
            }
        }
    }

    private String removeMarkdownFence(String content) {
        String cleaned = content == null ? "" : content.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring("```json".length()).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring("```".length()).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        return cleaned;
    }

    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return content.substring(start, end + 1).trim();
        }

        return content;
    }

    private GeneratedLearningReport replaceOverview(
            GeneratedLearningReport report,
            LearningReportSnapshot snapshot
    ) {
        if (report == null) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID, "AI返回的报告不能为空");
        }

        return new GeneratedLearningReport(
                report.title(),
                report.summary(),
                report.performanceLevel(),
                snapshot.overview(),
                report.highlights(),
                report.strengths(),
                report.weaknesses(),
                report.studyPlan(),
                report.encouragement(),
                report.disclaimer()
        );
    }

    private String buildUserPrompt(String snapshotJson, String schemaVersion) {
        return """
                请根据以下LearningReportSnapshot生成学习报告。

                输出要求：
                1. 输出结构版本：%s
                2. 只返回一个JSON对象，禁止Markdown代码块和任何额外文字
                3. strengths和weaknesses中的知识点ID及掌握度必须来自输入
                4. studyPlan中的courseId、chapterId和pointId必须来自candidateCourses
                5. 没有候选课程时不得编造ID
                6. 学习计划日期使用yyyy-MM-dd格式
                7. 学习计划序号从1开始连续递增
                8. 新用户报告不得声称存在长期趋势

                LearningReportSnapshot：
                %s
                """.formatted(schemaVersion, snapshotJson);
    }

    private boolean isConnectionOrTimeout(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof ConnectException
                    || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

    private void sleepBeforeRetry(int retryNumber) {
        long delayMillis = Math.min(1000L * (1L << (retryNumber - 1)), 5000L);

        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_UNAVAILABLE,
                    "AI请求重试被中断",
                    exception
            );
        }
    }

    private BusinessException convertHttpException(RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        String message = switch (statusCode) {
            case 400 -> "AI接口请求参数不正确";
            case 401 -> "AI接口认证失败，请检查API Key";
            case 403 -> "当前API Key没有模型访问权限";
            case 404 -> "AI接口地址或模型不存在";
            case 429 -> "AI接口请求频繁或额度不足";
            default -> statusCode >= 500
                    ? "AI供应商服务暂时不可用"
                    : "AI接口请求失败，状态码：" + statusCode;
        };

        return new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE, message, exception);
    }

    private Integer extractInputTokens(ChatCompletionResponse response) {
        return response.usage() == null ? null : response.usage().promptTokens();
    }

    private Integer extractOutputTokens(ChatCompletionResponse response) {
        return response.usage() == null ? null : response.usage().completionTokens();
    }

    private String resolveRequestId(ChatCompletionResponse response) {
        return response.id() == null || response.id().isBlank()
                ? UUID.randomUUID().toString()
                : response.id();
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private void validateArguments(
            LearningReportSnapshot snapshot,
            ReportGenerationOptions options
    ) {
        if (snapshot == null || snapshot.overview() == null) {
            throw new IllegalArgumentException("学习数据快照和概览不能为空");
        }
        if (options == null) {
            throw new IllegalArgumentException("报告生成参数不能为空");
        }
    }
}
