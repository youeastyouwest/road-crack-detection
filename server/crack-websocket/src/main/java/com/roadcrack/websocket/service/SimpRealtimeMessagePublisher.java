package com.roadcrack.websocket.service;

import com.roadcrack.api.response.detection.DetectionProgressMessage;
import com.roadcrack.api.response.detection.DetectionResultResponse;
import com.roadcrack.api.response.websocket.AlertMessageResponse;
import com.roadcrack.api.response.workorder.WorkOrderResponse;
import com.roadcrack.service.port.RealtimeMessagePublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SimpRealtimeMessagePublisher implements RealtimeMessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public SimpRealtimeMessagePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishDetectionProgress(Long taskId, DetectionProgressMessage message) {
        messagingTemplate.convertAndSend("/topic/detection/" + taskId + "/progress", message);
    }

    @Override
    public void publishDetectionResult(Long taskId, DetectionResultResponse result) {
        messagingTemplate.convertAndSend("/topic/detection/" + taskId + "/result", result);
    }

    @Override
    public void publishWorkOrderUpdate(WorkOrderResponse workOrder) {
        messagingTemplate.convertAndSend("/topic/work-orders", workOrder);
    }

    @Override
    public void publishAlert(AlertMessageResponse message) {
        messagingTemplate.convertAndSend("/topic/alerts", message);
    }
}
