package com.susan.training.advanced.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 教学版关键词检索器，生产环境可替换为 Elasticsearch 向量与 BM25 混合检索。
 */
@Component
@ConditionalOnProperty(
        prefix = "demo.chapter7",
        name = "enabled",
        havingValue = "true"
)
public class KnowledgeRetriever {
    private final List<KnowledgeDocument> documents = List.of(
            new KnowledgeDocument(
                    "KB-REFUND",
                    "订单退款规则",
                    "订单签收后 7 天内可以申请退款，定制商品和已激活数字商品除外。",
                    List.of("退款", "签收", "退货", "售后")
            ),
            new KnowledgeDocument(
                    "KB-INVOICE",
                    "电子发票申请",
                    "订单完成后可在个人中心申请电子发票，发票将在 2 个工作日内开具。",
                    List.of("发票", "电子发票", "开票")
            ),
            new KnowledgeDocument(
                    "KB-SHIPPING",
                    "物流与配送时效",
                    "现货订单通常在 24 小时内出库，偏远地区配送时间会增加 1 到 3 天。",
                    List.of("物流", "配送", "发货", "快递")
            )
    );

    public List<KnowledgeDocument> search(String question) {
        String normalized = question == null ? "" : question.trim();
        return documents.stream()
                .map(document -> new ScoredDocument(
                        document,
                        score(document, normalized)
                ))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator
                        .comparingInt(ScoredDocument::score)
                        .reversed())
                .limit(2)
                .map(ScoredDocument::document)
                .toList();
    }

    private int score(KnowledgeDocument document, String question) {
        int score = 0;
        for (String keyword : document.keywords()) {
            if (question.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private record ScoredDocument(KnowledgeDocument document, int score) {
    }
}
