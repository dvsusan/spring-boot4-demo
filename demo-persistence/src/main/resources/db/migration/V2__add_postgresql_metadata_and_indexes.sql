-- JSONB 保存渠道等弹性字段，生成列与 GIN 索引服务商品全文检索。
ALTER TABLE purchase_orders
    ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE order_items
    ADD COLUMN search_vector TSVECTOR
        GENERATED ALWAYS AS (
            to_tsvector('simple', product_name)
        ) STORED;

CREATE INDEX idx_order_tenant_customer_created
    ON purchase_orders (tenant_id, customer_id, created_at DESC);

CREATE INDEX idx_order_metadata_gin
    ON purchase_orders USING GIN (metadata jsonb_path_ops);

CREATE INDEX idx_order_item_search_gin
    ON order_items USING GIN (search_vector);
