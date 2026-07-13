package com.roadcrack.api.request.detection;

import java.util.List;

/**
 * 批量删除检测结果请求
 */
public record BatchDeleteRequest(
        List<Long> taskIds
) {
}
