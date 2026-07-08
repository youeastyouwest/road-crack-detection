package com.roadcrack.api.request.detection;

import com.roadcrack.api.enums.DataSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDetectionTaskRequest(
        @NotNull DataSourceType dataSourceType,
        @NotBlank String fileName,
        @NotBlank String fileUrl,
        @NotBlank String location,
        String remark
) {
}
