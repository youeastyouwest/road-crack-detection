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
            return "unknown";
        }
        return switch (damageType) {
            case CRACK -> "crack";
            case TRANSVERSE_CRACK -> "transverse_crack";
            case LONGITUDINAL_CRACK -> "longitudinal_crack";
            case NET_CRACK -> "net_crack";
            case MARKING_DAMAGE -> "marking_damage";
            case ROAD_SPILL -> "road_spill";
            case POTHOLE -> "pothole";
            case UNKNOWN -> "unknown_damage";
        };
    }

    static String severityLabel(SeverityLevel severityLevel) {
        if (severityLevel == null) {
            return "unknown";
        }
        return switch (severityLevel) {
            case LOW -> "low";
            case MEDIUM -> "medium";
            case HIGH -> "high";
        };
    }

    static String statusLabel(WorkOrderStatus status) {
        if (status == null) {
            return "unknown";
        }
        return switch (status) {
            case PENDING_ASSIGNMENT -> "pending_assignment";
            case ASSIGNED -> "assigned";
            case IN_PROGRESS -> "in_progress";
            case COMPLETED -> "completed";
            case PENDING_DEPT_REVIEW -> "pending_dept_review";
            case PENDING_ADMIN_REVIEW -> "pending_admin_review";
            case REJECTED -> "rejected";
            case CLOSED -> "closed";
            case CANCELLED -> "cancelled";
        };
    }

    static String departmentLabel(DepartmentCode departmentCode) {
        if (departmentCode == null) {
            return "unassigned";
        }
        return switch (departmentCode) {
            case ROAD_ADMIN -> "road_admin";
            case SANITATION -> "sanitation";
            case TRAFFIC_POLICE -> "traffic_police";
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
