-- 修复 road 表 damage_level 默认值
-- 将默认值从 'LOW' 改为 'HEALTHY'，与 RoadHealthScoreCalculator 输出对齐
ALTER TABLE road MODIFY COLUMN damage_level VARCHAR(16) DEFAULT 'HEALTHY' COMMENT '病害等级：HEALTHY/SUB_HEALTHY/UNHEALTHY';

-- 修复 road_health_archive 表 damage_level 默认值
ALTER TABLE road_health_archive MODIFY COLUMN damage_level VARCHAR(16) DEFAULT 'HEALTHY' COMMENT '病害等级：HEALTHY/SUB_HEALTHY/UNHEALTHY';

-- 将已有数据中 damage_level 旧值映射为新值
UPDATE road SET damage_level = 'HEALTHY' WHERE damage_level = 'LOW';
UPDATE road SET damage_level = 'UNHEALTHY' WHERE damage_level = 'HIGH';
UPDATE road SET damage_level = 'SUB_HEALTHY' WHERE damage_level = 'MEDIUM';

UPDATE road_health_archive SET damage_level = 'HEALTHY' WHERE damage_level = 'LOW';
UPDATE road_health_archive SET damage_level = 'UNHEALTHY' WHERE damage_level = 'HIGH';
UPDATE road_health_archive SET damage_level = 'SUB_HEALTHY' WHERE damage_level = 'MEDIUM';
