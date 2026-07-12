package com.roadcrack.service.service.impl;

import com.roadcrack.api.response.auditlog.AuditLogResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.AuditLogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuditLogService implements AuditLogService {

    private final List<AuditLogResponse> logs = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong idGen = new AtomicLong(0);

    public InMemoryAuditLogService() {
        // 种子审计日志数据
        seedLogs();
    }

    private void seedLogs() {
        addLog("admin", "USER", "LOGIN", "管理员登录系统", "127.0.0.1", 120L, "SUCCESS", null);
        addLog("admin", "USER", "CREATE", "创建用户：张巡查 (zhang_inspect)", "127.0.0.1", 85L, "SUCCESS", null);
        addLog("admin", "USER", "UPDATE", "修改用户状态：roadadmin 禁用 -> 启用", "127.0.0.1", 43L, "SUCCESS", null);
        addLog("admin", "DEPARTMENT", "CREATE", "创建部门：道路巡检一科", "127.0.0.1", 56L, "SUCCESS", null);
        addLog("admin", "ROLE", "UPDATE", "修改角色描述：ROLE_MAINTAINER 维修人员", "127.0.0.1", 38L, "SUCCESS", null);
        addLog("admin", "WORK_ORDER", "CREATE", "创建工单：WO-20250711-000001 (裂缝维修)", "127.0.0.1", 156L, "SUCCESS", null);
        addLog("admin", "WORK_ORDER", "ASSIGN", "指派工单 WO-20250711-000001 给道路巡检科", "127.0.0.1", 72L, "SUCCESS", null);
        addLog("admin", "DETECTION", "CREATE", "上传检测图片并启动AI检测", "127.0.0.1", 3420L, "SUCCESS", null);
        addLog("admin", "USER", "DELETE", "删除用户：test_user", "127.0.0.1", 34L, "SUCCESS", null);
        addLog("admin", "USER", "RESET_PASSWORD", "重置用户密码：zhang_inspect", "127.0.0.1", 29L, "SUCCESS", null);
        addLog("admin", "DEPARTMENT", "DELETE", "尝试删除部门：测试部门 (有关联用户)", "127.0.0.1", 15L, "FAILED", "部门下存在关联用户，无法删除");
        addLog("admin", "ROLE", "CREATE", "创建角色：ROLE_AUDITOR 审计员", "127.0.0.1", 47L, "SUCCESS", null);
        addLog("admin", "SYSTEM", "CONFIG", "修改系统配置：检测间隔 60s -> 30s", "127.0.0.1", 22L, "SUCCESS", null);
        addLog("admin", "WORK_ORDER", "CLOSE", "关闭工单 WO-20250711-000001 (维修完成)", "127.0.0.1", 68L, "SUCCESS", null);
        addLog("admin", "REPORT", "CREATE", "提交维修报告：MR-20250711-000001", "127.0.0.1", 134L, "SUCCESS", null);
    }

    private void addLog(String operator, String module, String action, String description,
                        String ip, Long duration, String status, String errorMsg) {
        AuditLogResponse log = new AuditLogResponse();
        log.setId(idGen.incrementAndGet());
        log.setOperator(operator);
        log.setAction(action);
        log.setTarget(module);
        log.setDetail(description);
        log.setIp(ip);
        log.setDuration(duration);
        log.setStatus(status);
        log.setCreatedAt(LocalDateTime.now().minusMinutes(idGen.get()));
        logs.add(log);
    }

    @Override
    public void record(String operator, String module, String action, String description,
                       String ip, Long duration, String status, String errorMsg) {
        AuditLogResponse log = new AuditLogResponse();
        log.setId(idGen.incrementAndGet());
        log.setOperator(operator != null ? operator : "system");
        log.setAction(action);
        log.setTarget(module);
        log.setDetail(description);
        log.setIp(ip != null ? ip : "0.0.0.0");
        log.setDuration(duration != null ? duration : 0L);
        log.setStatus(status);
        log.setCreatedAt(LocalDateTime.now());
        logs.add(0, log); // 新日志插到头部
    }

    @Override
    public PageResponse<AuditLogResponse> page(int page, int size, String module, String status) {
        List<AuditLogResponse> filtered = new ArrayList<>(logs);

        // 按模块筛选
        if (module != null && !module.trim().isEmpty()) {
            filtered = filtered.stream()
                    .filter(l -> module.equalsIgnoreCase(l.getTarget()) || module.equalsIgnoreCase(l.getAction()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 按状态筛选
        if (status != null && !status.trim().isEmpty()) {
            filtered = filtered.stream()
                    .filter(l -> status.equalsIgnoreCase(l.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 按时间倒序
        filtered.sort(Comparator.comparing(AuditLogResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int total = filtered.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<AuditLogResponse> pageList = start >= total ? Collections.emptyList() : new ArrayList<>(filtered.subList(start, end));

        return new PageResponse<>(pageList, total, size, page, (long) Math.ceil((double) total / size));
    }
}
