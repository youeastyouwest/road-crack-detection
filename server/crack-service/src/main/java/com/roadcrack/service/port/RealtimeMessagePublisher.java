package com.roadcrack.service.port;

import com.roadcrack.api.response.detection.DetectionProgressMessage;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.websocket.AlertMessageResponse;
import com.roadcrack.api.response.workorder.WorkOrderResponse;

public interface RealtimeMessagePublisher {

    void publishDetectionProgress(Long taskId, DetectionProgressMessage message);

    void publishDetectionResult(Long taskId, DetectionResultResponse result);

    void publishWorkOrderUpdate(WorkOrderResponse workOrder);

    void publishAlert(AlertMessageResponse message);
}
