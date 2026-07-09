package com.roadcrack.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
import com.roadcrack.api.request.report.CreateMaintenanceReportRequest;
import com.roadcrack.api.request.workorder.AssignWorkOrderRequest;
import com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.WorkOrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "crack.persistence.mode=memory",
        "crack.algorithm.mock-enabled=true"
})
class AuditLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void auditLogPageShouldReturnCoreWorkflowLogs() throws Exception {
        long taskId = createTask();
        executeTask(taskId);
        waitForDetectionResult(taskId);

        long workOrderId = firstWorkOrderId();
        assignWorkOrder(workOrderId);
        updateWorkOrderStatus(workOrderId, WorkOrderStatus.IN_PROGRESS, "repair started");
        updateWorkOrderStatus(workOrderId, WorkOrderStatus.COMPLETED, "repair completed");
        createMaintenanceReport(workOrderId);

        MvcResult result = mockMvc.perform(get("/api/audit-log/page")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(8)))
                .andReturn();

        Set<String> modules = new HashSet<>();
        Set<String> actions = new HashSet<>();
        for (JsonNode item : readBody(result).path("data").path("records")) {
            modules.add(item.path("module").asText());
            actions.add(item.path("action").asText());
        }

        assertTrue(modules.contains("DETECTION_TASK"));
        assertTrue(modules.contains("WORK_ORDER"));
        assertTrue(modules.contains("MAINTENANCE_REPORT"));
        assertTrue(actions.contains("CREATE"));
        assertTrue(actions.contains("COMPLETE"));
        assertTrue(actions.contains("ASSIGN"));
        assertTrue(actions.contains("STATUS_CHANGE"));
    }

    private long createTask() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/detection-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDetectionTaskRequest(
                                DataSourceType.MANUAL_IMAGE,
                                "audit-log-road-crack.jpg",
                                "http://example.com/audit-log-road-crack.jpg",
                                "Changsha Yuelu Avenue",
                                "audit log test"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return readBody(result).path("data").path("id").asLong();
    }

    private void executeTask(long taskId) throws Exception {
        mockMvc.perform(post("/api/detection-tasks/{taskId}/execute", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private long firstWorkOrderId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/work-orders")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andReturn();
        return readBody(result).path("data").path("records").get(0).path("id").asLong();
    }

    private void assignWorkOrder(long workOrderId) throws Exception {
        mockMvc.perform(put("/api/work-orders/{workOrderId}/assign", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignWorkOrderRequest(
                                DepartmentCode.ROAD_ADMIN,
                                "alice"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private void updateWorkOrderStatus(long workOrderId, WorkOrderStatus status, String note) throws Exception {
        mockMvc.perform(put("/api/work-orders/{workOrderId}/status", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateWorkOrderStatusRequest(status, note))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private void createMaintenanceReport(long workOrderId) throws Exception {
        mockMvc.perform(post("/api/maintenance-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMaintenanceReportRequest(
                                workOrderId,
                                "alice",
                                "http://example.com/before.jpg",
                                "http://example.com/after.jpg",
                                "asphalt",
                                "report created during audit log integration test",
                                LocalDateTime.now()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private void waitForDetectionResult(long taskId) throws Exception {
        for (int i = 0; i < 30; i++) {
            MvcResult result = mockMvc.perform(get("/api/detection-tasks/{taskId}/result", taskId))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode body = readBody(result);
            if (body.path("code").asInt() == 200
                    && !body.path("data").isMissingNode()
                    && !body.path("data").isNull()) {
                return;
            }
            Thread.sleep(100L);
        }
        throw new AssertionError("detection result not ready for task " + taskId);
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
