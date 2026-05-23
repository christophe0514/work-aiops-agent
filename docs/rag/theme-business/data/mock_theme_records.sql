-- Mock data for OperationQaAgent RAG and tool-call testing.
-- These records are intentionally simplified and should not be used as production DDL.

CREATE TABLE mock_theme_record (
    theme_id VARCHAR(32) PRIMARY KEY,
    theme_name VARCHAR(128) NOT NULL,
    merchant_id VARCHAR(32) NOT NULL,
    merchant_name VARCHAR(128) NOT NULL,
    theme_status VARCHAR(32) NOT NULL,
    audit_status VARCHAR(32) NOT NULL,
    publish_status VARCHAR(32) NOT NULL,
    sale_status VARCHAR(32) NOT NULL,
    channel_list VARCHAR(256),
    terminal_list VARCHAR(128),
    reject_reason VARCHAR(512),
    publish_failure_reason VARCHAR(512),
    updated_time DATETIME NOT NULL
);

INSERT INTO mock_theme_record VALUES
('TH202605220001','星夜蓝动态主题','M10001','灵感视觉工作室','ONLINE','REVIEW_APPROVED','PUBLISHED','SALEABLE','主题商店','Android',NULL,NULL,'2026-05-21 10:22:15'),
('TH202605220002','春日花语会员主题','M10008','青禾设计','ONLINE','REVIEW_APPROVED','PUBLISHED','MEMBER_ONLY','会员中心','Android',NULL,NULL,'2026-05-21 11:04:33'),
('TH202605220003','赛博霓虹锁屏','M10012','蓝鲸互动','APPROVED','REVIEW_APPROVED','NOT_SCHEDULED','NOT_FOR_SALE',NULL,'Android',NULL,'未配置上架渠道','2026-05-21 14:18:07'),
('TH202605220004','复古胶片桌面','M10003','拾光主题社','APPROVED','REVIEW_APPROVED','PUBLISH_FAILED','NOT_FOR_SALE','主题商店','iOS',NULL,'商品中心同步失败：价格未配置','2026-05-21 15:36:41'),
('TH202605220005','国风山海经','M10006','山海文化传媒','REJECTED','REVIEW_REJECTED','NOT_SCHEDULED','NOT_FOR_SALE',NULL,'Android','版权授权材料过期',NULL,'2026-05-20 19:02:12');
