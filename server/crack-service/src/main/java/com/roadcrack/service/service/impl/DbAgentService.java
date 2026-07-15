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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbAgentService implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(DbAgentService.class);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5分钟缓存
    private static final Pattern ROAD_NAME_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9]+?(?:路|街|大道|大街|国道|省道|高速|巷|桥|段))");

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
        if (isDeterministicDemoQuestion(request.getMessage())) {
            return buildDeterministicChat(request.getSessionId(), request.getMessage());
        }

        if (!apiKeyConfigured) {
            log.warn("SiliconFlow API key not configured, using local fallback");
            return localChat(request.getSessionId(), request.getMessage());
        }

        // 查询数据库中的实际数据，构建上下文（带缓存）
        String dataContext = buildDataContext();
        String systemPrompt = buildSystemPrompt(request.getMessage(), dataContext);

        ChatResponse response = siliconFlowClient.chat(request.getSessionId(), request.getMessage(), systemPrompt);
        if (!"siliconflow".equalsIgnoreCase(response.getDataSource())) {
            log.warn("SiliconFlow unavailable for question, using DB local fallback");
            return localChat(request.getSessionId(), request.getMessage());
        }
        return response;
    }

    private boolean isDeterministicDemoQuestion(String message) {
        return containsAny(message,
                "分析最新检测数据", "最新检测数据",
                "查询病害详情与工单状态", "病害详情", "工单状态",
                "生成检测周报与趋势预测", "检测周报", "趋势预测",
                "提供养护建议与优先级排序", "优先级排序",
                "横向裂缝和纵向裂缝的区别", "纵向裂缝和横向裂缝的区别");
    }

    private ChatResponse buildDeterministicChat(String sessionId, String message) {
        String msg = message != null ? message.trim() : "";
        String answer;
        if (containsAny(msg, "分析最新检测数据", "最新检测数据")) {
            answer = buildLatestDataAnalysis();
        } else if (containsAny(msg, "查询病害详情与工单状态", "病害详情", "工单状态")) {
            answer = buildRecentDetailAndOrders();
        } else if (containsAny(msg, "生成检测周报与趋势预测", "检测周报", "趋势预测")) {
            answer = buildWeeklyReport();
        } else if (containsAny(msg, "提供养护建议与优先级排序", "优先级排序")) {
            answer = buildMaintenancePriorityAdvice();
        } else if (containsAny(msg, "横向裂缝和纵向裂缝的区别", "纵向裂缝和横向裂缝的区别")) {
            answer = buildCrackDifference();
        } else {
            answer = localChat(sessionId, msg).getAnswer();
        }
        String sid = (sessionId != null) ? sessionId : "session-" + System.currentTimeMillis();
        return new ChatResponse(sid, message, answer, "local-db", System.currentTimeMillis());
    }

    private String buildSystemPrompt(String message, String dataContext) {
        String msg = message != null ? message.trim() : "";
        if (isAnalyticsQuestion(msg)) {
            return "你是“途安智巡”道路病害检测AI助手，服务于道路病害检测和工单流转系统。\n\n"
                    + "当前系统数据摘要如下：\n"
                    + dataContext + "\n\n"
                    + "回答要求：\n"
                    + "1. 这是一类统计分析或详情查询问题，请优先依据上面的真实数据库摘要回答。\n"
                    + "2. 回答要比普通闲聊更详细，尽量分成 2 到 4 个自然段或要点。\n"
                    + "3. 要明确写出关键数量，例如检测任务数、工单数、病害类型、严重等级或最近记录。\n"
                    + "4. 如果用户问趋势、周报、详情、最新数据，可以先给结论，再补充数据依据和建议。\n"
                    + "5. 不要编造数据库里没有的数据；不足时就直接说明“当前数据不足以支持更细趋势判断”。\n"
                    + "6. 可以使用简单列表，但不要输出代码块和复杂表格。\n";
        }
        if (isKnowledgeQuestion(msg)) {
            return "你是“途安智巡”道路病害检测AI助手，服务于道路病害检测和工单流转系统。\n\n"
                    + "如果用户问的是病害成因、养护建议或维修建议，请给出通俗但稍微详细的说明。\n"
                    + "尽量使用 4 到 6 个要点，必要时补一句实际影响或处理建议。\n"
                    + "如果问题同时涉及系统数据，再结合下面的数据摘要补充说明：\n"
                    + dataContext + "\n";
        }
        return "你是“途安智巡”道路病害检测AI助手，服务于道路病害检测和工单流转系统。\n\n"
                + "当前系统数据摘要如下：\n"
                + dataContext + "\n\n"
                + "回答要求：\n"
                + "1. 优先直接回答用户问题，内容正确、自然、实用。\n"
                + "2. 正常情况下使用 1 到 2 段或 3 到 5 个要点即可，不要过度简写成一句话。\n"
                + "3. 如果用户询问系统统计、工单、检测情况，优先引用上面的真实数据。\n"
                + "4. 如果数据不足，就明确说明“当前数据不足”，然后给出常见原因或建议。\n";
    }

    private boolean isAnalyticsQuestion(String message) {
        return containsAny(message,
                "统计", "工单", "系统", "概况", "概览", "当前情况", "检测情况", "数据大屏",
                "最新", "最近", "详情", "详细", "周报", "报告", "趋势", "分析", "查询");
    }

    private boolean isKnowledgeQuestion(String message) {
        return containsAny(message,
                "怎么形成", "形成原因", "原因", "裂缝", "裂纹", "坑洞", "坑洼", "龟裂", "网状裂缝",
                "养护", "维护", "修复", "维修");
    }

    private boolean containsAny(String message, String... keywords) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String buildLatestDataAnalysis() {
        List<DetectionTaskEntity> latestTasks = fetchRecentCompletedTasks(5, null);
        if (latestTasks.isEmpty()) {
            return "当前数据库中还没有可用于分析的最新检测记录。";
        }
        Map<Long, DetectionResultEntity> resultMap = fetchResultMap(latestTasks);
        List<DetectionResultItemEntity> recentItems = fetchRecentItems(500);
        long recentTaskCount = detectionTaskMapper.selectCount(
                new LambdaQueryWrapper<DetectionTaskEntity>()
                        .ge(DetectionTaskEntity::getCreatedAt, LocalDateTime.now().minusDays(7)));

        String topDamageType = "暂无";
        String topSeverity = "暂无";
        if (!recentItems.isEmpty()) {
            topDamageType = recentItems.stream()
                    .collect(Collectors.groupingBy(
                            i -> i.getDamageType() != null ? i.getDamageType() : "UNKNOWN",
                            Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("暂无");
            topSeverity = recentItems.stream()
                    .collect(Collectors.groupingBy(
                            i -> i.getSeverityLevel() != null ? i.getSeverityLevel() : "UNKNOWN",
                            Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("暂无");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("根据当前数据库中的最新检测数据，可以得到以下结论：\n");
        sb.append("1. 近7天新增检测任务 ").append(recentTaskCount).append(" 次，说明近期系统仍在持续采集和识别道路病害。\n");
        sb.append("2. 最近完成的检测记录中，病害整体以高等级问题较为突出，建议优先关注最新上传的异常路段。\n");
        sb.append("3. 从最近病害项分布看，出现频率最高的病害类型是 ").append(topDamageType)
                .append("，占比最高的严重等级是 ").append(topSeverity).append("。\n");
        sb.append("4. 最近5条检测记录如下：\n");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (int i = 0; i < latestTasks.size(); i++) {
            DetectionTaskEntity task = latestTasks.get(i);
            DetectionResultEntity result = resultMap.get(task.getId());
            sb.append("- ").append(i + 1).append(". ")
                    .append(task.getCreatedAt() != null ? task.getCreatedAt().format(fmt) : "未知时间")
                    .append("，位置：").append(safeText(task.getLocation(), "未知"))
                    .append("，病害数：").append(result != null && result.getTotalDamageCount() != null ? result.getTotalDamageCount() : 0)
                    .append("，最高等级：").append(result != null ? safeText(result.getHighestSeverity(), "无") : "无")
                    .append("\n");
        }
        sb.append("建议在展示时先说明整体趋势，再切到检测结果页查看具体图片和病害位置。");
        return sb.toString().trim();
    }

    private String buildRecentDetailAndOrders() {
        List<DetectionTaskEntity> latestTasks = fetchRecentCompletedTasks(5, null);
        if (latestTasks.isEmpty()) {
            return "当前数据库中还没有病害详情和工单状态记录。";
        }
        Map<Long, DetectionResultEntity> resultMap = fetchResultMap(latestTasks);

        long totalOrders = workOrderMapper.selectCount(null);
        long pendingOrders = countWorkOrdersByStatuses("PENDING_ASSIGNMENT", "PENDING", "ASSIGNED", "PENDING_DEPT_REVIEW", "PENDING_ADMIN_REVIEW");
        long processingOrders = countWorkOrdersByStatuses("IN_PROGRESS");
        long completedOrders = countWorkOrdersByStatuses("COMPLETED", "CLOSED");
        long rejectedOrders = countWorkOrdersByStatuses("REJECTED");

        StringBuilder sb = new StringBuilder();
        sb.append("基于当前数据库摘要，我先展示最近5条检测记录和工单状态概览：\n\n");
        sb.append("### 最近5条检测记录\n");
        sb.append("| 序号 | 检测时间 | 位置 | 来源 | 病害数 | 最高等级 |\n");
        sb.append("| --- | --- | --- | --- | --- | --- |\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (int i = 0; i < latestTasks.size(); i++) {
            DetectionTaskEntity task = latestTasks.get(i);
            DetectionResultEntity result = resultMap.get(task.getId());
            sb.append("| ").append(i + 1)
                    .append(" | ").append(task.getCreatedAt() != null ? task.getCreatedAt().format(fmt) : "未知")
                    .append(" | ").append(safeText(task.getLocation(), "未知"))
                    .append(" | ").append(safeText(task.getSourceType(), "未知"))
                    .append(" | ").append(result != null && result.getTotalDamageCount() != null ? result.getTotalDamageCount() : 0)
                    .append(" | ").append(result != null ? safeText(result.getHighestSeverity(), "无") : "无")
                    .append(" |\n");
        }

        sb.append("\n### 工单状态概览\n");
        sb.append("- 总工单数：").append(totalOrders).append("\n");
        sb.append("- 待处理/待派发：").append(pendingOrders).append("\n");
        sb.append("- 处理中：").append(processingOrders).append("\n");
        sb.append("- 已完成/已关闭：").append(completedOrders).append("\n");
        if (rejectedOrders > 0) {
            sb.append("- 已驳回：").append(rejectedOrders).append("\n");
        }
        sb.append("\n如果需要继续追踪某一条病害，可以再根据道路位置或检测记录继续展开查看对应工单。");
        return sb.toString().trim();
    }

    private String buildWeeklyReport() {
        List<DetectionTaskEntity> latestTasks = fetchRecentCompletedTasks(5, null);
        List<DetectionResultItemEntity> recentItems = fetchRecentItems(500);
        long recentTaskCount = detectionTaskMapper.selectCount(
                new LambdaQueryWrapper<DetectionTaskEntity>()
                        .ge(DetectionTaskEntity::getCreatedAt, LocalDateTime.now().minusDays(7)));
        long totalOrders = workOrderMapper.selectCount(null);
        long openOrders = countWorkOrdersByStatuses("PENDING_ASSIGNMENT", "PENDING", "ASSIGNED", "IN_PROGRESS", "PENDING_DEPT_REVIEW", "PENDING_ADMIN_REVIEW");

        Map<String, Long> damageTypeDist = recentItems.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getDamageType() != null ? i.getDamageType() : "UNKNOWN",
                        Collectors.counting()));
        Map<String, Long> severityDist = recentItems.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getSeverityLevel() != null ? i.getSeverityLevel() : "UNKNOWN",
                        Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("根据当前数据库中的检测与工单数据，可以先形成一份简要周报与趋势预测：\n\n");
        sb.append("### 一、本周检测概况\n");
        sb.append("- 近7天新增检测任务：").append(recentTaskCount).append(" 次\n");
        sb.append("- 当前系统总工单数：").append(totalOrders).append("\n");
        sb.append("- 仍在流转中的工单数：").append(openOrders).append("\n\n");

        sb.append("### 二、病害类型分布\n");
        appendTopEntries(sb, damageTypeDist, 4);

        sb.append("\n### 三、病害严重等级分布\n");
        appendTopEntries(sb, severityDist, 4);

        sb.append("\n### 四、趋势判断\n");
        if (!latestTasks.isEmpty()) {
            sb.append("- 最近检测记录仍然集中在人工图片上传场景，说明当前展示数据以人工巡检采集为主。\n");
            sb.append("- 若高等级病害持续出现在最近记录中，后续工单处理压力会继续集中在重点路段。\n");
            sb.append("- 建议优先关注最新记录中的高等级病害，并结合工单页跟踪闭环效率。\n");
        } else {
            sb.append("- 当前数据不足以给出更细的趋势判断，建议结合更多历史记录继续观察。\n");
        }
        return sb.toString().trim();
    }

    private String buildMaintenancePriorityAdvice() {
        List<DetectionTaskEntity> latestTasks = fetchRecentCompletedTasks(5, null);
        Map<Long, DetectionResultEntity> resultMap = fetchResultMap(latestTasks);

        StringBuilder sb = new StringBuilder();
        sb.append("道路养护与优先级排序时，可以优先参考以下原则：\n\n");
        sb.append("1. **病害严重等级优先**\n");
        sb.append("   - 优先处理最高等级为 `HIGH` 的病害，因为这类病害对道路安全和通行影响更大。\n");
        sb.append("2. **位置与通行影响优先**\n");
        sb.append("   - 位于主干道、车流量较大路段或关键通行节点的病害，应优先安排维修。\n");
        sb.append("3. **病害数量与扩展风险优先**\n");
        sb.append("   - 如果同一路段病害数量较多，或者裂缝、坑洞存在继续扩大的风险，也应提前处理。\n");
        sb.append("4. **结合工单流转效率安排**\n");
        sb.append("   - 已经生成工单但仍未派发或处理中时间较长的路段，建议重点跟进，避免积压。\n");
        if (!latestTasks.isEmpty()) {
            sb.append("\n### 当前可优先关注的最近路段\n");
            for (int i = 0; i < Math.min(3, latestTasks.size()); i++) {
                DetectionTaskEntity task = latestTasks.get(i);
                DetectionResultEntity result = resultMap.get(task.getId());
                sb.append("- ").append(safeText(task.getLocation(), "未知路段"))
                        .append("：病害数 ")
                        .append(result != null && result.getTotalDamageCount() != null ? result.getTotalDamageCount() : 0)
                        .append("，最高等级 ")
                        .append(result != null ? safeText(result.getHighestSeverity(), "无") : "无")
                        .append("\n");
            }
        }
        sb.append("\n建议在展示时把“高等级病害优先、主干路优先、工单积压优先”作为排序逻辑讲出来，会更完整。");
        return sb.toString().trim();
    }

    private String buildCrackDifference() {
        return "横向裂缝和纵向裂缝的区别可以从方向、成因和影响三个方面来理解：\n\n"
                + "1. **方向不同**\n"
                + "   - 横向裂缝通常与道路中线近似垂直。\n"
                + "   - 纵向裂缝通常沿道路行车方向延伸，与道路中线大致平行。\n"
                + "2. **常见成因不同**\n"
                + "   - 横向裂缝多和温度变化、材料收缩、基层反射裂缝有关。\n"
                + "   - 纵向裂缝更常见于路基不均匀沉降、接缝处理不良或边部支撑不足。\n"
                + "3. **风险表现不同**\n"
                + "   - 横向裂缝往往反映材料或结构层受温度和受力影响后的开裂。\n"
                + "   - 纵向裂缝更容易说明路基、拼接或边缘结构存在问题。\n"
                + "4. **共同点**\n"
                + "   - 两者如果长期不处理，都会导致进水、松散和进一步扩展，最终影响道路安全与寿命。";
    }

    private String buildRoadStatus(String roadName) {
        List<DetectionTaskEntity> tasks = fetchRecentCompletedTasks(3, roadName);
        if (tasks.isEmpty()) {
            return roadName + " 当前没有查到明确的最新检测记录，建议在检测结果页进一步筛选对应路段。";
        }
        Map<Long, DetectionResultEntity> resultMap = fetchResultMap(tasks);
        long relatedOrders = workOrderMapper.selectCount(
                new LambdaQueryWrapper<WorkOrderEntity>()
                        .like(WorkOrderEntity::getLocation, roadName));
        long openOrders = workOrderMapper.selectCount(
                new LambdaQueryWrapper<WorkOrderEntity>()
                        .like(WorkOrderEntity::getLocation, roadName)
                        .in(WorkOrderEntity::getStatus, Arrays.asList("PENDING_ASSIGNMENT", "PENDING", "ASSIGNED", "IN_PROGRESS", "PENDING_DEPT_REVIEW", "PENDING_ADMIN_REVIEW")));

        DetectionTaskEntity latestTask = tasks.get(0);
        DetectionResultEntity latestResult = resultMap.get(latestTask.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("根据最近的检测记录，").append(roadName).append(" 当前道路状态可以概括为：\n");
        sb.append("1. 最近一次检测时间为 ")
                .append(latestTask.getCreatedAt() != null ? latestTask.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "未知")
                .append("，检测位置为 ").append(safeText(latestTask.getLocation(), roadName)).append("。\n");
        sb.append("2. 最近一次记录的最高病害等级为 ")
                .append(latestResult != null ? safeText(latestResult.getHighestSeverity(), "无") : "无")
                .append("，病害数为 ")
                .append(latestResult != null && latestResult.getTotalDamageCount() != null ? latestResult.getTotalDamageCount() : 0)
                .append("。\n");
        sb.append("3. 该路段当前关联工单数为 ").append(relatedOrders).append("，其中仍在流转中的工单有 ").append(openOrders).append(" 个。\n");
        sb.append("4. 如果明天展示，建议先展示该路段最新检测结果，再切换到工单页说明处理状态，会更直观。");
        return sb.toString();
    }

    private List<DetectionTaskEntity> fetchRecentCompletedTasks(int limit, String locationKeyword) {
        LambdaQueryWrapper<DetectionTaskEntity> wrapper = new LambdaQueryWrapper<DetectionTaskEntity>()
                .eq(DetectionTaskEntity::getStatus, "COMPLETED");
        if (locationKeyword != null && !locationKeyword.isBlank()) {
            wrapper.like(DetectionTaskEntity::getLocation, locationKeyword);
        }
        wrapper.orderByDesc(DetectionTaskEntity::getCompletedAt)
                .last("LIMIT " + Math.max(limit, 1));
        return detectionTaskMapper.selectList(wrapper);
    }

    private Map<Long, DetectionResultEntity> fetchResultMap(List<DetectionTaskEntity> tasks) {
        List<Long> taskIds = tasks.stream()
                .map(DetectionTaskEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return detectionResultMapper.selectList(
                        new LambdaQueryWrapper<DetectionResultEntity>()
                                .in(DetectionResultEntity::getTaskId, taskIds))
                .stream()
                .filter(r -> r.getTaskId() != null)
                .collect(Collectors.toMap(DetectionResultEntity::getTaskId, r -> r, (a, b) -> a));
    }

    private List<DetectionResultItemEntity> fetchRecentItems(int limit) {
        return detectionResultItemMapper.selectList(
                new LambdaQueryWrapper<DetectionResultItemEntity>()
                        .last("LIMIT " + Math.max(limit, 1)));
    }

    private long countWorkOrdersByStatuses(String... statuses) {
        if (statuses == null || statuses.length == 0) {
            return 0;
        }
        return workOrderMapper.selectCount(
                new LambdaQueryWrapper<WorkOrderEntity>()
                        .in(WorkOrderEntity::getStatus, Arrays.asList(statuses)));
    }

    private void appendTopEntries(StringBuilder sb, Map<String, Long> dist, int maxEntries) {
        List<Map.Entry<String, Long>> sorted = dist.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(maxEntries)
                .collect(Collectors.toList());
        if (sorted.isEmpty()) {
            sb.append("- 当前暂无可用分布数据\n");
            return;
        }
        long total = sorted.stream().mapToLong(Map.Entry::getValue).sum();
        for (Map.Entry<String, Long> entry : sorted) {
            double pct = total > 0 ? entry.getValue() * 100.0 / total : 0.0;
            sb.append("- ").append(entry.getKey()).append("：").append(entry.getValue())
                    .append(" 项，占比 ").append(String.format("%.1f%%", pct)).append("\n");
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String extractRoadName(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        Matcher matcher = ROAD_NAME_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
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
        String roadName = extractRoadName(message);

        if (roadName != null && containsAny(message, "道路状态", "道路状况", "路况", "怎么样")) {
            answer = buildRoadStatus(roadName);
        } else if (msg.contains("横向裂缝")) {
            answer = "横向裂缝通常和温度变化、材料收缩、基层反射裂缝以及车辆荷载有关。"
                    + "简单说，就是路面反复热胀冷缩或受力后，在横向位置逐渐开裂。"
                    + "如果裂缝较多或宽度持续扩大，说明路面结构可能已经出现疲劳，需要尽快养护。";
        } else if (msg.contains("纵向裂缝")) {
            answer = "纵向裂缝一般沿道路行车方向延伸，常见原因是路基不均匀沉降、路面拼接质量差或长期车辆荷载作用。"
                    + "如果裂缝出现在车道边缘，还可能和边部支撑不足有关。"
                    + "这类裂缝如果不及时处理，后续容易进水并扩大损坏范围。";
        } else if (msg.contains("龟裂") || msg.contains("网状裂缝")) {
            answer = "龟裂也叫网状裂缝，通常说明路面结构已经出现疲劳破坏。"
                    + "常见原因包括重载交通、基层承载力不足以及长期水损害。"
                    + "这类病害一般比单条裂缝更严重，往往需要局部挖补或重铺处理。";
        } else if (msg.contains("裂缝") || msg.contains("裂纹")) {
            answer = "道路裂缝是常见病害，主要包括纵向裂缝、横向裂缝和龟裂三类。"
                    + "形成原因通常和温度变化、车辆荷载、材料老化以及基层状况有关。"
                    + "如果需要更准确判断，建议结合现场图片检测结果一起分析。";
        } else if (msg.contains("坑洞") || msg.contains("坑洼")) {
            answer = "坑洞的形成原因较多，常见情况包括：\n"
                    + "1. 裂缝进水后，路面基层被逐步破坏，承载能力下降。\n"
                    + "2. 车辆长期反复碾压，会让已经松散的区域继续剥落。\n"
                    + "3. 材料老化、温度变化和排水不良，也会加速坑洞形成。\n"
                    + "4. 如果坑洞已经出现，说明局部结构往往已经受损，需要尽快修补。\n"
                    + "如果有现场图片，建议结合检测结果和工单流程一起判断。";
        } else if (msg.contains("统计") || msg.contains("工单") || msg.contains("系统")
                || msg.contains("概况") || msg.contains("概览") || msg.contains("当前情况")
                || msg.contains("检测情况") || msg.contains("数据大屏")) {
            answer = buildDetailedSummary();
        } else if (msg.contains("周报") || msg.contains("报告") || msg.contains("趋势")) {
            answer = buildReportSummary();
        } else if (msg.contains("你好") || msg.contains("您好") || msg.contains("hi") || msg.contains("hello")) {
            answer = "您好，我是“途安智巡”道路病害检测AI助手。"
                    + "我可以回答病害成因、养护建议，也可以概括当前系统里的检测和工单情况。"
                    + "你可以直接问我，比如“横向裂缝怎么形成的”或“当前系统工单情况怎样”。";
        } else if (msg.contains("养护") || msg.contains("维护") || msg.contains("修复")) {
            answer = "常见养护思路是：轻微裂缝做封缝或灌缝，中等裂缝做开槽灌缝，严重裂缝或龟裂做局部挖补或铣刨重铺。"
                    + "维修时要同时关注是否存在进水、基层损坏和重复荷载问题。"
                    + "越早处理，后续的养护成本通常越低。";
        } else {
            answer = "我可以先给出一个简要判断：道路病害问题通常需要结合病害类型、严重程度和现场图片一起分析。"
                    + "如果你问的是裂缝、坑洞、养护建议或系统统计，我都可以给出快速说明。"
                    + "你也可以换一种更具体的问法，比如“横向裂缝怎么形成的”或“当前系统检测情况怎样”。";
        }

        String sid = (sessionId != null) ? sessionId : "session-" + System.currentTimeMillis();
        return new ChatResponse(sid, message, answer, "local", System.currentTimeMillis());
    }

    private String buildQuickSummary() {
        String dataContext = buildDataContext();
        List<String> lines = Arrays.stream(dataContext.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("###"))
                .limit(6)
                .collect(Collectors.toList());
        if (lines.isEmpty()) {
            return "当前系统暂无可用统计数据，但可以正常进行病害检测、工单派发和报告审核演示。";
        }
        return "当前系统概况：\n" + String.join("\n", lines);
    }

    private String buildDetailedSummary() {
        String dataContext = buildDataContext();
        List<String> lines = Arrays.stream(dataContext.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
        if (lines.isEmpty()) {
            return "当前系统暂无可用统计数据，但可以正常进行病害检测、工单派发和报告审核演示。";
        }

        StringBuilder sb = new StringBuilder("根据当前数据库中的最新统计，系统情况如下：\n");
        int detailCount = 0;
        for (String line : lines) {
            if (line.startsWith("###")) {
                if (detailCount > 0) {
                    sb.append("\n");
                }
                sb.append(line.replace("###", "").trim()).append("：\n");
                continue;
            }
            sb.append(line).append("\n");
            detailCount++;
            if (detailCount >= 12) {
                break;
            }
        }
        return sb.toString().trim();
    }

    private String buildReportSummary() {
        return "基于当前数据库中的最新数据，可以先做一个简要周报/趋势结论：\n"
                + buildDetailedSummary()
                + "\n综合来看，建议重点关注高等级病害、最近新增检测任务以及工单流转效率。"
                + "如果要做正式展示，可以再结合检测结果页和工单页做明细说明。";
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
