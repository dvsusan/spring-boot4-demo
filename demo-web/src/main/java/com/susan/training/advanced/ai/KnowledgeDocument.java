package com.susan.training.advanced.ai;

import java.util.List;

/**
 * 进入检索上下文的企业知识片段。
 */
public record KnowledgeDocument(
        String id,
        String title,
        String content,
        List<String> keywords
) {
}
