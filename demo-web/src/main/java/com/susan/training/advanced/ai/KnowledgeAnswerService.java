package com.susan.training.advanced.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 组合知识检索、Prompt 构造和 Spring AI ChatClient。
 */
@Service
@ConditionalOnProperty(
        prefix = "demo.chapter7",
        name = "enabled",
        havingValue = "true"
)
public class KnowledgeAnswerService {
    private static final String SYSTEM_PROMPT = """
            你是企业客服助手。
            只能根据提供的知识库资料回答。
            如果资料不足，必须明确说“当前知识库没有足够信息”，不能猜测。
            回答控制在 120 字以内，并给出引用来源标题。
            """;

    private final KnowledgeRetriever retriever;
    private final ChatClient chatClient;

    public KnowledgeAnswerService(
            KnowledgeRetriever retriever,
            ChatClient chatClient
    ) {
        this.retriever = retriever;
        this.chatClient = chatClient;
    }

    public KnowledgeAnswer answer(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        List<KnowledgeDocument> documents = retriever.search(question);
        if (documents.isEmpty()) {
            return new KnowledgeAnswer(
                    "当前知识库没有足够信息，请联系人工客服。",
                    List.of(),
                    false
            );
        }

        String context = documents.stream()
                .map(document -> "【" + document.title() + "】\n"
                        + document.content())
                .reduce("", (left, right) -> left + right + "\n");
        String userPrompt = """
                知识库资料：
                %s

                用户问题：
                %s
                """.formatted(context, question);

        try {
            String content = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
            return new KnowledgeAnswer(
                    content,
                    documents.stream().map(KnowledgeDocument::title).toList(),
                    true
            );
        } catch (RuntimeException exception) {
            // 模型不可用时保留检索结果，允许前端降级为知识片段展示。
            return new KnowledgeAnswer(
                    "AI 服务暂不可用，请先查看知识库原文。",
                    documents.stream().map(KnowledgeDocument::title).toList(),
                    false
            );
        }
    }

    public record KnowledgeAnswer(
            String answer,
            List<String> sources,
            boolean aiAnswered
    ) {
    }
}
