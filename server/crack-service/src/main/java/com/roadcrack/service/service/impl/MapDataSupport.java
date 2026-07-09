package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class MapDataSupport {

    private static final BigDecimal DEMO_CENTER_LNG = BigDecimal.valueOf(112.938814D);
    private static final BigDecimal DEMO_CENTER_LAT = BigDecimal.valueOf(28.228209D);

    private MapDataSupport() {
    }

    static String damageTypeLabel(DamageType damageType) {
        if (damageType == null) {
            return "未分类";
        }
        return switch (damageType) {
            case CRACK -> "裂缝";
            case MARKING_DAMAGE -> "标线破损";
            case ROAD_SPILL -> "路面抛洒";
            case POTHOLE -> "坑槽";
            case UNKNOWN -> "未知病害";
        };
    }

    static String severityLabel(SeverityLevel severityLevel) {
        if (severityLevel == null) {
            return "未知";
        }
        return switch (severityLevel) {
            case LOW -> "低";
            case MEDIUM -> "中";
            case HIGH -> "高";
        };
    }

    static String statusLabel(WorkOrderStatus status) {
        if (status == null) {
            return "未生成工单";
        }
        return switch (status) {
            case PENDING_ASSIGNMENT -> "待派发";
            case ASSIGNED -> "已派发";
            case IN_PROGRESS -> "处理中";
            case COMPLETED -> "已完成";
            case CLOSED -> "已关闭";
            case CANCELLED -> "已取消";
        };
    }

    static String departmentLabel(DepartmentCode departmentCode) {
        if (departmentCode == null) {
            return "未分配";
        }
        return switch (departmentCode) {
            case ROAD_ADMIN -> "道路管理";
            case SANITATION -> "环卫部门";
            case TRAFFIC_POLICE -> "交管部门";
        };
    }

    static boolean containsIgnoreCase(String source, String keyword) {
        return source != null
                && keyword != null
                && source.toLowerCase().contains(keyword.toLowerCase());
    }

    static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    static double ratio(long part, long total) {
        if (total <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf(part * 100D / total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    static BigDecimal demoLng(long taskId, int itemIndex) {
        long seed = Math.abs(taskId * 37 + itemIndex * 17L);
        BigDecimal offset = BigDecimal.valueOf((seed % 120) / 1000D);
        return DEMO_CENTER_LNG.add(offset).setScale(6, RoundingMode.HALF_UP);
    }

    static BigDecimal demoLat(long taskId, int itemIndex) {
        long seed = Math.abs(taskId * 29 + itemIndex * 13L);
        BigDecimal offset = BigDecimal.valueOf((seed % 80) / 1000D);
        return DEMO_CENTER_LAT.add(offset).setScale(6, RoundingMode.HALF_UP);
    }
}
