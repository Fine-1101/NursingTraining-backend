package org.example.nursingtrainingbackend.modules.learningreport.ai.openai;


/**
 * OpenAI兼容对话消息。
 *
 * @param role    消息角色，例如system、user、assistant
 * @param content 消息内容
 */
public record ChatMessage(
        String role,
        String content
) {

    /**
     * 创建系统消息。
     */
    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    /**
     * 创建用户消息。
     */
    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    /**
     * 创建AI助手消息。
     */
    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }

    /**
     * 校验消息内容。
     */
    public ChatMessage {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "消息角色不能为空"
            );
        }

        if (!role.equals("system")
                && !role.equals("user")
                && !role.equals("assistant")) {
            throw new IllegalArgumentException(
                    "不支持的消息角色：" + role
            );
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "消息内容不能为空"
            );
        }
    }
}