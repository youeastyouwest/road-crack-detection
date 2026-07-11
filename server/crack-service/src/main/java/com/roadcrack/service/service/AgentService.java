package com.roadcrack.service.service;

import com.roadcrack.api.request.agent.ChatRequest;
import com.roadcrack.api.request.agent.DetectImageRequest;
import com.roadcrack.api.request.agent.GenerateReportRequest;
import com.roadcrack.api.response.agent.AgentReportResponse;
import com.roadcrack.api.response.agent.ChatResponse;
import com.roadcrack.api.response.agent.DetectImageResponse;

public interface AgentService {
    ChatResponse chat(ChatRequest request);
    DetectImageResponse detectImage(byte[] imageBytes, String filename, DetectImageRequest request);
    AgentReportResponse generateReport(GenerateReportRequest request);
}