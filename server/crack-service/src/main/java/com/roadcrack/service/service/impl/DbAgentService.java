package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roadcrack.api.request.agent.ChatRequest;
import com.roadcrack.api.request.agent.DetectImageRequest;
import com.roadcrack.api.request.agent.GenerateReportRequest;
import com.roadcrack.api.response.agent.AgentReportResponse;
import com.roadcrack.api.response.agent.ChatResponse;
import com.roadcrack.api.response.agent.DetectImageResponse;
import com.roadcrack.dao.entity.*;
import com.roadcrack.dao.mapper.*;
import com.roadcrack.service.client.SiliconFlowClient;
import com.roadcrack.service.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbAgentService implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(DbAgentService.class);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5分钟缓存

    private final SiliconFlowClient siliconFlowClient;
    private final boolean apiKeyConfigured;

    // 数据查询依赖
    private final DetectionTaskMapper detectionTaskMapper;
    private final DetectionResultMapper detectionResultMapper;
    private final DetectionResultItemMapper detectionResultItemMapper;
    private final WorkOrderMapper workOrderMapper;
    private final RoadMapper roadMapper;

    // 缓存
    private volatile String cachedDataContext = null;
    private volatile long cachedAt = 0;

    public DbAgentService(
            SiliconFlowClient siliconFlowClient,
            @Value("${crack.agent.siliconflow.api-key:}") String apiKey,
            DetectionTaskMapper detectionTaskMapper,
            DetectionResultMapper detectionResultMapper,
            DetectionResultItemMapper detectionResultItemMapper,
            WorkOrderMapper workOrderMapper,
            RoadMapper roadMapper) {
        this.siliconFlowClient = siliconFlowClient;
        this.apiKeyConfigured = (apiKey != null && !apiKey.isEmpty());
        this.detectionTaskMapper = detectionTaskMapper;
        this.detectionResultMapper = detectionResultMapper;
        this.detectionResultItemMapper = detectionResultItemMapper;
        this.workOrderMapper = workOrderMapper;
        this.roadMapper = roadMapper;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (!apiKeyConfigured) {
            log.warn("SiliconFlow API key not configured, using local fallback");
            return localChat(request.getSessionId(), request.getMessage());
        }

        // 查询数据库中的实际数据，构建上下文（带缓存）
        String dataContext = buildDataContext();

        String systemPrompt = "你是\"途安智巡\"道路病害检测AI助手。你正在为一个道路病害检测系统提供智能问答服务。\n\n"
                + "## 系统能力\n"
                + "- AI自动检测道路裂缝、坑洞、标线损坏、路面抛洒等病害\n"
                + "- 自动生成工单并分配给相关部门\n"
                + "- 数据大屏实时展示病害统计和地图分布\n"
                + "- 道路健康档案管理\n\n"
                + "## 当前系统数据摘要\n"
                + dataContext + "\n\n"
                + "## 回答规则\n"
                + "1. 基于上面的真实数据回答问题，不要编造不存在的数据\n"
                + "2. 如果用户要求生成周报/报告，请基于数据摘要生成结构化的报告，包含：概述、数据统计、趋势分析、建议\n"
                + "3. 如果用户要求趋势预测，基于已有数据合理推演，并说明依据\n"
                + "4. 使用 Markdown 格式输出，善用表格、列表让内容更清晰\n"
                + "5. 数字数据请精确引用上面提供的数据\n"
                + "6. 如果数据不足以支撑某个分析，诚实说明\n"
                + "7. 保持专业、简洁、有实用价值\n";

        return siliconFlowClient.chat(request.getSessionId(), request.getMessage(), systemPrompt);
    }

    /**
     * 查询数据库，构建当前系统的数据摘要，作为 AI 的上下文。
     * 带 5 分钟缓存，避免每次对话都查数据库。
     */
    private String buildDataContext() {
        long now = System.currentTimeMillis();
        if (cachedDataContext != null && (now - cachedAt) < CACHE_TTL_MS) {
            return cachedDataContext;
        }

        StringBuilder sb = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        try {
            // 1. 检测任务统计 — 用 selectCount 代替 selectList
            Long totalTasks = detectionTaskMapper.selectCount(null);
            Long completedTasks = detectionTaskMapper.selectCount(
                    new LambdaQueryWrapper<DetectionTaskEntity>()
                            .eq(DetectionTaskEntity::getStatus, "COMPLETED"));
            Long pendingTasks = detectionTaskMapper.selectCount(
                    new LambdaQueryWrapper<DetectionTaskEntity>()
                            .eq(DetectionTaskEntity::getStatus, "PENDING"));
            Long failedTasks = detectionTaskMapper.selectCount(
                    new LambdaQueryWrapper<DetectionTaskEntity>()
                            .eq(DetectionTaskEntity::getStatus, "FAILED"));

            sb.append("### 检测任务\n");
            sb.append("- 总检测任务数: ").append(totalTasks).append("\n");
            sb.append("- 已完成: ").append(completedTasks).append("，待处理: ").append(pendingTasks).append("，失败: ").append(failedTasks).append("\n");

            // 近7天检测任务数 — 只查 count，不拉全量数据
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            Long recentTaskCount = detectionTaskMapper.selectCount(
                    new LambdaQueryWrapper<DetectionTaskEntity>()
                            .ge(DetectionTaskEntity::getCreatedAt, sevenDaysAgo));
            sb.append("- 近7天新增检测任务: ").append(recentTaskCount).append("次\n");

            // 2. 病害检测结果统计 — 只查 count，不拉全量数据到内存
            Long totalResults = detectionResultMapper.selectCount(null);
            sb.append("\n### 病害检测结果\n");
            sb.append("- 总检测结果数: ").append(totalResults).append("\n");

            // 病害类型分布 — 限制最多查 500 条，避免全表扫描
            List<DetectionResultItemEntity> allItems = detectionResultItemMapper.selectList(
                    new LambdaQueryWrapper<DetectionResultItemEntity>()
                            .last("LIMIT 500"));
            sb.append("\n### 病害类型分布\n");
            sb.append("- 病害项数(最近500条): ").append(allItems.size()).append("\n");
            if (!allItems.isEmpty()) {
                Map<String, Long> damageTypeDist = allItems.stream()
                        .collect(Collectors.groupingBy(
                                i -> i.getDamageType() != null ? i.getDamageType() : "UNKNOWN",
                                Collectors.counting()));
                damageTypeDist.forEach((k, v) -> {
                    double pct = v * 100.0 / allItems.size();
                    sb.append("- ").append(k).append(": ").append(v).append("个 (")
                            .append(String.format("%.1f%%", pct)).append(")\n");
                });

                // 严重等级分布
                Map<String, Long> itemSeverityDist = allItems.stream()
                        .collect(Collectors.groupingBy(
                                i -> i.getSeverityLevel() != null ? i.getSeverityLevel() : "UNKNOWN",
                                Collectors.counting()));
                sb.append("- 病害严重等级分布:\n");
                itemSeverityDist.forEach((k, v) -> {
                    double pct = v * 100.0 / allItems.size();
                    sb.append("  - ").append(k).append(": ").append(v).append("个 (")
                            .append(String.format("%.1f%%", pct)).append(")\n");
                });
            }

            // 3. 工单统计 — 4 次 count 查询
            Long totalOrders = workOrderMapper.selectCount(null);
            Long pendingOrders = workOrderMapper.selectCount(
                    new LambdaQueryWrapper<WorkOrderEntity>()
                            .eq(WorkOrderEntity::getStatus, "PENDING"));
            Long inProgressOrders = workOrderMapper.selectCount(
                    new LambdaQueryWrapper<WorkOrderEntity>()
                            .eq(WorkOrderEntity::getStatus, "IN_PROGRESS"));
            Long completedOrders = workOrderMapper.selectCount(
                    new LambdaQueryWrapper<WorkOrderEntity>()
                            .eq(WorkOrderEntity::getStatus, "COMPLETED"));

            sb.append("\n### 工单统计\n");
            sb.append("- 总工单数: ").append(totalOrders).append("\n");
            sb.append("- 待处理: ").append(pendingOrders).append("，处理中: ").append(inProgressOrders)
                    .append("，已完成: ").append(completedOrders).append("\n");

            Long autoOrders = workOrderMapper.selectCount(
                    new LambdaQueryWrapper<WorkOrderEntity>()
                            .eq(WorkOrderEntity::getSourceType, "AUTO_CRACK"));
            sb.append("- AI自动生成工单: ").append(autoOrders).append("\n");

            // 4. 道路统计 — 只查 count + 限制条数
            Long totalRoads = roadMapper.selectCount(null);
            sb.append("\n### 道路数据\n");
            sb.append("- 管养道路总数: ").append(totalRoads).append("条\n");

            // 5. 最近的检测记录 — 批量查询消除 N+1
            List<DetectionTaskEntity> latestTasks = detectionTaskMapper.selectList(
                    new LambdaQueryWrapper<DetectionTaskEntity>()
                            .eq(DetectionTaskEntity::getStatus, "COMPLETED")
                            .orderByDesc(DetectionTaskEntity::getCompletedAt)
                            .last("LIMIT 5"));
            if (!latestTasks.isEmpty()) {
                // 批量查询关联结果，消除 N+1
                List<Long> taskIds = latestTasks.stream()
                        .map(DetectionTaskEntity::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                Map<Long, DetectionResultEntity> resultMap = new HashMap<>();
                if (!taskIds.isEmpty()) {
                    List<DetectionResultEntity> results = detectionResultMapper.selectList(
                            new LambdaQueryWrapper<DetectionResultEntity>()
                                    .in(DetectionResultEntity::getTaskId, taskIds));
                    resultMap = results.stream()
                            .filter(r -> r.getTaskId() != null)
                            .collect(Collectors.toMap(DetectionResultEntity::getTaskId, r -> r, (a, b) -> a));
                }

                sb.append("\n### 最近5条检测记录\n");
                for (DetectionTaskEntity t : latestTasks) {
                    sb.append("- ").append(t.getCreatedAt() != null ? t.getCreatedAt().format(fmt) : "未知时间")
                            .append(" | 位置: ").append(t.getLocation() != null ? t.getLocation() : "未知")
                            .append(" | 来源: ").append(t.getSourceType() != null ? t.getSourceType() : "未知");
                    DetectionResultEntity result = (t.getId() != null) ? resultMap.get(t.getId()) : null;
                    if (result != null) {
                        sb.append(" | 病害数: ").append(result.getTotalDamageCount() != null ? result.getTotalDamageCount() : 0)
                                .append(" | 最高等级: ").append(result.getHighestSeverity() != null ? result.getHighestSeverity() : "无");
                    }
                    sb.append("\n");
                }
            }

            sb.append("\n### 时间\n");
            sb.append("- 当前时间: ").append(LocalDateTime.now().format(fmt)).append("\n");

        } catch (Exception e) {
            log.error("Failed to build data context: {}", e.getMessage(), e);
            sb.append("(数据查询异常: ").append(e.getMessage()).append(")\n");
        }

        String result = sb.toString();
        cachedDataContext = result;
        cachedAt = System.currentTimeMillis();
        return result;
    }

    /**
     * 本地降级回复：当 API Key 未配置或 API 不可用时使用关键词匹配生成回复。
     */
    private ChatResponse localChat(String sessionId, String message) {
        String msg = (message != null) ? message.trim().toLowerCase() : "";
        String answer;

        if (msg.contains("裂缝") || msg.contains("裂纹")) {
            answer = "道路裂缝是常见病害，主要分为纵向裂缝、横向裂缝和龟裂（网状裂缝）三种。\n\n"
                    + "- 纵向裂缝：平行于道路中心线，通常因路基不均匀沉降引起\n"
                    + "- 横向裂缝：垂直于道路中心线，多由温度变化或半刚性基层反射导致\n"
                    + "- 龟裂：网状裂缝，通常表明路面结构层已严重疲劳\n\n"
                    + "您可以通过\"数据采集\"页面上传图片进行AI自动检测识别。";
        } else if (msg.contains("坑洞") || msg.contains("坑洼")) {
            answer = "坑洞是路面局部破损后形成的凹陷，通常由裂缝未及时修补发展而来。\n\n"
                    + "- 坑洞会严重影响行车安全和舒适性\n"
                    + "- 建议上传现场图片进行AI检测评估严重程度\n"
                    + "- 严重坑洞应立即设置警示标志并安排修复";
        } else if (msg.contains("周报") || msg.contains("报告") || msg.contains("趋势")) {
            answer = "检测周报和趋势分析功能需要接入AI大模型才能生成详细报告。\n\n"
                    + "当前AI助手尚未配置大模型API Key，无法生成报告。\n"
                    + "请在 application.yml 中配置 crack.agent.siliconflow.api-key 后使用此功能。";
        } else if (msg.contains("你好") || msg.contains("您好") || msg.contains("hi") || msg.contains("hello")) {
            answer = "您好！我是\"途安智巡\"道路病害检测AI助手。\n\n"
                    + "我可以帮您：\n"
                    + "- 解答道路病害相关问题\n"
                    + "- 生成检测周报与趋势预测\n"
                    + "- 分析裂缝类型和成因\n"
                    + "- 提供养护建议\n\n"
                    + "请问有什么可以帮您的？";
        } else if (msg.contains("养护") || msg.contains("维护") || msg.contains("修复")) {
            answer = "道路裂缝养护建议：\n\n"
                    + "- 轻微裂缝（<3mm）：表面封闭处理，如灌缝或贴缝\n"
                    + "- 中等裂缝（3-10mm）：开槽灌缝，清除裂缝两侧松散材料后灌注密封胶\n"
                    + "- 严重裂缝（>10mm）或龟裂：铣刨重铺，局部挖补后重新摊铺\n\n"
                    + "建议定期巡检，早发现早处治，可大幅降低养护成本。";
        } else {
            answer = "您好！我是\"途安智巡\"道路病害检测AI助手。\n\n"
                    + "您可以向我询问：\n"
                    + "- 生成检测周报与趋势预测\n"
                    + "- 道路裂缝的类型和成因\n"
                    + "- 坑洞的危害和处治方法\n"
                    + "- 裂缝养护维修建议\n"
                    + "- 查看数据大屏统计\n\n"
                    + "请描述您遇到的问题，我会尽力为您解答。";
        }

        String sid = (sessionId != null) ? sessionId : "session-" + System.currentTimeMillis();
        return new ChatResponse(sid, message, answer, "local", System.currentTimeMillis());
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
