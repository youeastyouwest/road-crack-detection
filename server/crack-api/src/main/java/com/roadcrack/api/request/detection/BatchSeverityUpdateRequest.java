package com.roadcrack.api.request.detection;

import java.util.List;

/**
 * 批量更新检测结果严重等级请求
 */
public record BatchSeverityUpdateRequest(
        List<Long> taskIds,
        String newSeverity
) {
}
