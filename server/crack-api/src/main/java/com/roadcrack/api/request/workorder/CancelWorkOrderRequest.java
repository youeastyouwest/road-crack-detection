package com.roadcrack.api.request.workorder;

import jakarta.validation.constraints.NotBlank;

public record CancelWorkOrderRequest(
        @NotBlank String reason
) {
}
