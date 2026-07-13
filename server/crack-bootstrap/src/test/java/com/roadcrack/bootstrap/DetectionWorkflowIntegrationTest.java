package com.roadcrack.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
import com.roadcrack.api.request.report.CreateMaintenanceReportRequest;
import com.roadcrack.api.request.workorder.AssignWorkOrderRequest;
import com.roadcrack.api.request.workorder.CreateWorkOrderRequest;
import com.roadcrack.api.request.workorder.UpdateWorkOrderStatusRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class DetectionWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void detectionTaskExecutionShouldGenerateResultAndWorkOrder() throws Exception {
        Long taskId = createDetectionTask();

        mockMvc.perform(post("/api/detection-tasks/{taskId}/execute", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        JsonNode resultData = waitForDetectionResult(taskId);
        assertTrue(resultData.path("items").isArray());
        assertTrue(resultData.path("items").size() >= 1);

        long workOrderId = resultData.path("generatedWorkOrderId").asLong();
        assertTrue(workOrderId > 0);

        mockMvc.perform(get("/api/work-orders/{workOrderId}", workOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(workOrderId))
                .andExpect(jsonPath("$.data.detectionTaskId").value(taskId))
                .andExpect(jsonPath("$.data.status").value(WorkOrderStatus.PENDING_ASSIGNMENT.name()));
    }

    @Test
    void maintenanceReportShouldCloseCompletedWorkOrder() throws Exception {
        long workOrderId = createManualWorkOrder();
        assignWorkOrder(workOrderId);
        updateWorkOrderStatus(workOrderId, WorkOrderStatus.IN_PROGRESS, "arrived on site");
        updateWorkOrderStatus(workOrderId, WorkOrderStatus.COMPLETED, "repair finished");

        CreateMaintenanceReportRequest request = new CreateMaintenanceReportRequest(
                workOrderId,
                "lisi",
                "http://example.com/before.jpg",
                "http://example.com/after.jpg",
                "asphalt material",
                "repair completed",
                LocalDateTime.now()
        );

        mockMvc.perform(post("/api/maintenance-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.workOrderId").value(workOrderId))
                .andExpect(jsonPath("$.data.executor").value("lisi"));

        mockMvc.perform(get("/api/work-orders/{workOrderId}", workOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(WorkOrderStatus.CLOSED.name()));
    }

    @Test
    void maintenanceReportShouldRejectNonCompletedWorkOrder() throws Exception {
        long workOrderId = createManualWorkOrder();

        CreateMaintenanceReportRequest request = new CreateMaintenanceReportRequest(
                workOrderId,
                "wangwu",
                null,
                null,
                null,
                "report submitted before work order completion",
                LocalDateTime.now()
        );

        mockMvc.perform(post("/api/maintenance-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    private Long createDetectionTask() throws Exception {
        CreateDetectionTaskRequest request = new CreateDetectionTaskRequest(
                DataSourceType.MANUAL_IMAGE,
                "road-crack-01.jpg",
                "http://example.com/road-crack-01.jpg",
                "Changsha test road",
                "test task",
                null
        );

        MvcResult result = mockMvc.perform(post("/api/detection-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode body = readBody(result);
        JsonNode data = body.path("data");
        assertNotNull(data);
        return data.path("id").asLong();
    }

    private long createManualWorkOrder() throws Exception {
        CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                999L,
                "manual-test-work-order",
                DamageType.CRACK,
                SeverityLevel.HIGH,
                "Changsha test section",
                DepartmentCode.ROAD_ADMIN,
                "http://example.com/evidence.jpg",
                "test work order description"
        );

        MvcResult result = mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        return readBody(result).path("data").path("id").asLong();
    }

    private void assignWorkOrder(long workOrderId) throws Exception {
        AssignWorkOrderRequest request = new AssignWorkOrderRequest(DepartmentCode.ROAD_ADMIN, "zhangsan");
        mockMvc.perform(put("/api/work-orders/{workOrderId}/assign", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(WorkOrderStatus.ASSIGNED.name()));
    }

    private void updateWorkOrderStatus(long workOrderId, WorkOrderStatus statusValue, String note) throws Exception {
        UpdateWorkOrderStatusRequest request = new UpdateWorkOrderStatusRequest(statusValue, note);
        mockMvc.perform(put("/api/work-orders/{workOrderId}/status", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(statusValue.name()));
    }

    private JsonNode waitForDetectionResult(Long taskId) throws Exception {
        JsonNode latestBody = null;
        for (int i = 0; i < 30; i++) {
            MvcResult result = mockMvc.perform(get("/api/detection-tasks/{taskId}/result", taskId))
                    .andExpect(status().isOk())
                    .andReturn();

            latestBody = readBody(result);
            if (latestBody.path("code").asInt() == 200 && !latestBody.path("data").isMissingNode() && !latestBody.path("data").isNull()) {
                return latestBody.path("data");
            }
            Thread.sleep(100L);
        }

        assertNotNull(latestBody);
        assertEquals(200, latestBody.path("code").asInt(), latestBody.toPrettyString());
        return latestBody.path("data");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
