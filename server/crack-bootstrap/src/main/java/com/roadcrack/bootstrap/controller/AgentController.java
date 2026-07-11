package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.request.agent.ChatRequest;
import com.roadcrack.api.request.agent.DetectImageRequest;
import com.roadcrack.api.request.agent.GenerateReportRequest;
import com.roadcrack.api.response.agent.AgentReportResponse;
import com.roadcrack.api.response.agent.ChatResponse;
import com.roadcrack.api.response.agent.DetectImageResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.service.service.AgentService;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * AI Agent 智能服务控制器
 * <p>
 * 提供智能问答、图片检测建议、自动报告生成接口。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse chatResponse = agentService.chat(request);
        return ApiResponse.success(chatResponse);
    }

    @PostMapping(value = "/detect-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DetectImageResponse> detectImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "question", required = false) String question,
            @RequestParam(value = "generateAdvice", required = false, defaultValue = "true") boolean generateAdvice,
            @RequestParam(value = "autoGenerateWorkOrder", required = false, defaultValue = "true") boolean autoGenerateWorkOrder,
            @RequestParam(value = "autoDispatch", required = false, defaultValue = "true") boolean autoDispatch,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "confidenceThreshold", required = false) Double confidenceThreshold
    ) throws IOException {
        DetectImageRequest request = new DetectImageRequest();
        request.setQuestion(question);
        request.setGenerateAdvice(generateAdvice);
        request.setAutoGenerateWorkOrder(autoGenerateWorkOrder);
        request.setAutoDispatch(autoDispatch);
        request.setConfidenceThreshold(confidenceThreshold);

        DetectImageResponse response = agentService.detectImage(
                image.getBytes(),
                location != null ? location : image.getOriginalFilename(),
                request
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/report")
    public ApiResponse<AgentReportResponse> generateReport(@Valid @RequestBody GenerateReportRequest request) {
        AgentReportResponse reportResponse = agentService.generateReport(request);
        return ApiResponse.success(reportResponse);
    }
}
