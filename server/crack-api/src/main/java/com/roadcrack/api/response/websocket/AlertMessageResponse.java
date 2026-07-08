package com.roadcrack.api.response.websocket;

import java.time.LocalDateTime;

public record AlertMessageResponse(
        String type,
        String level,
        Long taskId,
        String title,
        String message,
        LocalDateTime timestamp
) {
}
