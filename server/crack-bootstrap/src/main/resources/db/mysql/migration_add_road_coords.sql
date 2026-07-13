-- Migration: 为 road 表添加中心点坐标字段
-- 用于检测任务创建时根据坐标自动匹配最近道路

ALTER TABLE `road`
    ADD COLUMN IF NOT EXISTS `center_lng` DECIMAL(10,7) COMMENT '中心点经度' AFTER `end_point`,
    ADD COLUMN IF NOT EXISTS `center_lat` DECIMAL(10,7) COMMENT '中心点纬度' AFTER `center_lng`;

-- 更新已有道路的中心点坐标（北京道路真实坐标）
UPDATE `road` SET `center_lng` = 116.4075, `center_lat` = 39.9070 WHERE `id` = 1 AND `center_lng` IS NULL;
UPDATE `road` SET `center_lng` = 116.4350, `center_lat` = 39.9150 WHERE `id` = 2 AND `center_lng` IS NULL;
UPDATE `road` SET `center_lng` = 116.4550, `center_lat` = 39.9150 WHERE `id` = 3 AND `center_lng` IS NULL;
UPDATE `road` SET `center_lng` = 116.4600, `center_lat` = 39.9200 WHERE `id` = 4 AND `center_lng` IS NULL;
UPDATE `road` SET `center_lng` = 116.4500, `center_lat` = 39.9080 WHERE `id` = 5 AND `center_lng` IS NULL;
UPDATE `road` SET `center_lng` = 116.4480, `center_lat` = 39.9000 WHERE `id` = 6 AND `center_lng` IS NULL;
