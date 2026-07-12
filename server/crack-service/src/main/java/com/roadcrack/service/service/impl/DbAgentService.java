package com.roadcrack.service.service.impl;

import com.roadcrack.api.request.agent.ChatRequest;
import com.roadcrack.api.request.agent.DetectImageRequest;
import com.roadcrack.api.request.agent.GenerateReportRequest;
import com.roadcrack.api.response.agent.AgentReportResponse;
import com.roadcrack.api.response.agent.ChatResponse;
import com.roadcrack.api.response.agent.DetectImageResponse;
import com.roadcrack.service.service.AgentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbAgentService implements AgentService {

    @Override
    public ChatResponse chat(ChatRequest request) {
        return new ChatResponse("session-1", request.getMessage(), "Agent DB mode not yet implemented. Please use memory mode or configure external AI service.", null, System.currentTimeMillis());
    }

    @Override
    public DetectImageResponse detectImage(byte[] imageBytes, String filename, DetectImageRequest request) {
        DetectImageResponse resp = new DetectImageResponse();
        resp.setTimestamp(System.currentTimeMillis());
        return resp;
    }

    @Override
    public AgentReportResponse generateReport(GenerateReportRequest request) {
        AgentReportResponse resp = new AgentReportResponse();
        resp.setReportId("report-1");
        resp.setReportType(request.getReportType());
        resp.setTitle("报告");
        resp.setContent("DB mode report generation not available.");
        resp.setTimestamp(System.currentTimeMillis());
        return resp;
    }
}
