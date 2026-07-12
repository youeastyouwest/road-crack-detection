package com.roadcrack.service.util;

import com.roadcrack.dao.entity.DetectionResultItemEntity;
import com.roadcrack.dao.entity.RoadEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 道路健康评分计算工具。
 *
 * 评分规则（满分 100，分数越低表示病害越严重）：
 * 1. 基础分 100
 * 2. 按病害密度扣分：每公里每处病害扣 3 分（上限 30 分）
 * 3. 按严重程度扣分：
 *    - HIGH  severity：每处扣 2 分
 *    - MEDIUM severity：每处扣 1 分
 *    - LOW severity：每处扣 0.3 分
 * 4. 按病害类型扣分：
 *    - 坑槽(POTHOLE)额外每处扣 1 分（结构性损坏）
 * 5. 最低分保底 10 分（不会降到 0）
 *
 * 病害等级划分：
 * - >= 85：HEALTHY（健康）
 * - >= 60：SUB_HEALTHY（亚健康）
 * - < 60：UNHEALTHY（不健康/需维修）
 */
public class RoadHealthScoreCalculator {

    private static final BigDecimal BASE_SCORE = BigDecimal.valueOf(100);
    private static final BigDecimal MIN_SCORE = BigDecimal.valueOf(10);

    /**
     * 根据道路信息和该道路的所有病害结果项，计算健康评分。
     *
     * @param road       道路实体（用于获取 length_km）
     * @param allItems   该道路下的所有 detection_result_item
     * @return 计算后的健康评分（10 ~ 100）
     */
    public static BigDecimal calculate(RoadEntity road, List<DetectionResultItemEntity> allItems) {
        if (allItems == null || allItems.isEmpty()) {
            return BASE_SCORE; // 无病害 = 满分
        }

        double lengthKm = road != null && road.getLengthKm() != null
                ? road.getLengthKm().doubleValue()
                : 1.0; // 默认 1km 避免除零

        int totalCount = allItems.size();
        int highCount = 0, mediumCount = 0, lowCount = 0, potholeCount = 0;

        for (DetectionResultItemEntity item : allItems) {
            String sev = item.getSeverityLevel();
            if ("HIGH".equals(sev)) highCount++;
            else if ("MEDIUM".equals(sev)) mediumCount++;
            else lowCount++;

            String dt = item.getDamageType();
            if ("POTHOLE".equals(dt)) potholeCount++;
        }

        // 1. 病害密度扣分 = (totalCount / lengthKm) * 3，上限 30
        double densityPenalty = Math.min((totalCount / lengthKm) * 3.0, 30.0);

        // 2. 严重程度扣分
        double severityPenalty = highCount * 2.0 + mediumCount * 1.0 + lowCount * 0.3;

        // 3. 坑槽额外扣分（结构性损坏）
        double potholePenalty = potholeCount * 1.0;

        double totalPenalty = densityPenalty + severityPenalty + potholePenalty;

        double score = BASE_SCORE.doubleValue() - totalPenalty;
        if (score < MIN_SCORE.doubleValue()) {
            score = MIN_SCORE.doubleValue();
        }

        return BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 根据评分判断病害等级。
     */
    public static String resolveDamageLevel(BigDecimal healthScore) {
        if (healthScore == null) return "UNKNOWN";
        int score = healthScore.intValue();
        if (score >= 85) return "HEALTHY";
        if (score >= 60) return "SUB_HEALTHY";
        return "UNHEALTHY";
    }
}
