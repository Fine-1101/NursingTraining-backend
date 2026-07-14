package org.example.nursingtrainingbackend.modules.learningreport.ai.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * OpenAI兼容对话补全请求。
 *
 * @param model       模型名称
 * @param messages    对话消息
 * @param temperature 模型生成随机度
 * @param maxTokens   最大输出Token数
 */
public record ChatCompletionRequest(

        String model,

        List<ChatMessage> messages,

        BigDecimal temperature,

        @JsonProperty("max_tokens")
        Integer maxTokens
) {

    /**
     * 请求参数校验。
     */
    public ChatCompletionRequest {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException(
                    "AI模型名称不能为空"
            );
        }

        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException(
                    "AI消息列表不能为空"
            );
        }

        /*
         * 防止调用者传入可修改的List，
         * 避免创建请求后内容又被修改。
         */
        messages = List.copyOf(messages);

        if (temperature == null) {
            throw new IllegalArgumentException(
                    "temperature不能为空"
            );
        }

        if (temperature.compareTo(BigDecimal.ZERO) < 0
                || temperature.compareTo(
                new BigDecimal("2")
        ) > 0) {
            throw new IllegalArgumentException(
                    "temperature必须在0到2之间"
            );
        }

        if (maxTokens == null || maxTokens < 100) {
            throw new IllegalArgumentException(
                    "maxTokens不能小于100"
            );
        }
    }
}