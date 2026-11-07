package com.susan.training.advanced.ai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * 无外部模型凭据时使用的确定性实现，只验证 Spring AI 调用链。
 */
public class DemoChatModel implements ChatModel {
    private final String model;

    public DemoChatModel(String model) {
        this.model = model;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        String userText = prompt.getUserMessage().getText();
        String excerpt = userText.length() <= 320
                ? userText
                : userText.substring(0, 320) + "...";
        // 固定文本让自动化测试可重复，不声称具备真实大模型理解能力。
        String answer = "演示模型（" + model + "）已收到知识库上下文：\n" + excerpt;
        return new ChatResponse(List.of(
                new Generation(new AssistantMessage(answer))
        ));
    }
}
