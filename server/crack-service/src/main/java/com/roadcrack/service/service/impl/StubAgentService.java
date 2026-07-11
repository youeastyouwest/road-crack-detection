package com.roadcrack.service.service.impl;

import com.roadcrack.api.request.agent.ChatRequest;
import com.roadcrack.api.request.agent.DetectImageRequest;
import com.roadcrack.api.request.agent.GenerateReportRequest;
import com.roadcrack.api.response.agent.AgentReportResponse;
import com.roadcrack.api.response.agent.ChatResponse;
import com.roadcrack.api.response.agent.DetectImageResponse;
import com.roadcrack.service.client.SiliconFlowClient;
import com.roadcrack.service.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StubAgentService implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(StubAgentService.class);
    private final SiliconFlowClient siliconFlowClient;
    private final boolean apiKeyConfigured;

    public StubAgentService(SiliconFlowClient siliconFlowClient,
            @Value("${crack.agent.siliconflow.api-key:}") String apiKey) {
        this.siliconFlowClient = siliconFlowClient;
        this.apiKeyConfigured = (apiKey != null && !apiKey.isEmpty());
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (!apiKeyConfigured) {
            return localChat(request.getSessionId(), request.getMessage());
        }
        String systemPrompt = "你是一个专业的道路病害检测AI助手，名为\"途安智巡\"。"
                + "你擅长分析道路裂缝、坑洞、标线损坏、路面抛洒等病害数据。"
                + "请用中文回答，保持专业简洁。"
                + "如果用户询问检测数据，引导他们查看数据大屏或上传图片检测。"
                + "如果用户询问技术问题，用专业但易懂的语言解释。";
        return siliconFlowClient.chat(request.getSessionId(), request.getMessage(), systemPrompt);
    }

    private ChatResponse localChat(String sessionId, String message) {
        String msg = (message != null) ? message.trim().toLowerCase() : "";
        String answer;

        if (msg.contains("裂缝") || msg.contains("裂纹")) {
            answer = "道路裂缝是常见病害，主要分为纵向裂缝、横向裂缝和龟裂（网状裂缝）三种。\n\n"
                    + "• **纵向裂缝**：平行于道路中心线，通常因路基不均匀沉降引起\n"
                    + "• **横向裂缝**：垂直于道路中心线，多由温度变化或半刚性基层反射导致\n"
                    + "• **龟裂**：网状裂缝，通常表明路面结构层已严重疲劳\n\n"
                    + "您可以通过\"数据采集\"页面上传图片进行AI自动检测识别。";
        } else if (msg.contains("坑洞") || msg.contains("坑洼")) {
            answer = "坑洞是路面局部破损后形成的凹陷，通常由裂缝未及时修补发展而来。\n\n"
                    + "• 坑洞会严重影响行车安全和舒适性\n"
                    + "• 建议上传现场图片进行AI检测评估严重程度\n"
                    + "• 严重坑洞应立即设置警示标志并安排修复";
        } else if (msg.contains("检测") || msg.contains("上传")) {
            answer = "您可以在\"数据采集\"页面上传道路图片或视频进行AI裂缝检测。\n\n"
                    + "支持格式：JPG/PNG/MP4\n"
                    + "检测内容：纵向裂缝、横向裂缝、龟裂、坑洞\n"
                    + "检测完成后，结果会显示在检测记录中，并同步到数据大屏。";
        } else if (msg.contains("你好") || msg.contains("您好") || msg.contains("hi") || msg.contains("hello")) {
            answer = "您好！我是\"途安智巡\"道路病害检测AI助手。\n\n"
                    + "我可以帮您：\n"
                    + "• 解答道路病害相关问题\n"
                    + "• 引导您进行图片上传检测\n"
                    + "• 分析裂缝类型和成因\n"
                    + "• 提供养护建议\n\n"
                    + "请问有什么可以帮您的？";
        } else if (msg.contains("养护") || msg.contains("维护") || msg.contains("修复")) {
            answer = "道路裂缝养护建议：\n\n"
                    + "• **轻微裂缝（<3mm）**：表面封闭处理，如灌缝或贴缝\n"
                    + "• **中等裂缝（3-10mm）**：开槽灌缝，清除裂缝两侧松散材料后灌注密封胶\n"
                    + "• **严重裂缝（>10mm）或龟裂**：铣刨重铺，局部挖补后重新摊铺\n\n"
                    + "建议定期巡检，早发现早处治，可大幅降低养护成本。";
        } else if (msg.contains("大屏") || msg.contains("数据") || msg.contains("统计")) {
            answer = "您可以在\"数据大屏\"页面查看实时的道路病害统计数据，包括：\n\n"
                    + "• 总里程与裂缝总数\n"
                    + "• 病害类型分布（饼图）\n"
                    + "• 严重等级分布\n"
                    + "• 本周检测趋势\n"
                    + "• 地图上的病害点位分布";
        } else {
            answer = "您好！我是\"途安智巡\"道路病害检测AI助手。\n\n"
                    + "您可以向我询问：\n"
                    + "• 道路裂缝的类型和成因\n"
                    + "• 坑洞的危害和处治方法\n"
                    + "• 如何上传图片进行检测\n"
                    + "• 裂缝养护维修建议\n"
                    + "• 查看数据大屏统计\n\n"
                    + "请描述您遇到的问题，我会尽力为您解答。";
        }

        String sid = (sessionId != null) ? sessionId : "session-" + System.currentTimeMillis();
        return new ChatResponse(sid, message, answer, "local", System.currentTimeMillis());
    }

    @Override
    public DetectImageResponse detectImage(byte[] imageBytes, String filename, DetectImageRequest request) {
        DetectImageResponse r = new DetectImageResponse();
        r.setTaskId("sf-" + System.currentTimeMillis());
        r.setHasCrack(false);
        r.setNumDetections(0);
        r.setAdvice("图片分析功能基于YOLOv8裂缝检测模型，请通过数据采集页面上传检测。");
        r.setDataSource("local");
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }

    @Override
    public AgentReportResponse generateReport(GenerateReportRequest request) {
        AgentReportResponse r = new AgentReportResponse();
        r.setReportId("sf-" + System.currentTimeMillis());
        r.setTitle("检测报告");
        r.setSummary("报告生成功能开发中，请先在检测记录中查看详细数据。");
        r.setReportType(request.getReportType() != null ? request.getReportType() : "general");
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }
}