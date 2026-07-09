package com.roadcrack.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.api.request.report.CreateMaintenanceReportRequest;
import com.roadcrack.api.request.workorder.AssignWorkOrderRequest;
import com.roadcrack.api.request.workorder.CancelWorkOrderRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class WorkflowQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listWorkOrdersShouldSupportStatusAndKeywordFilters() throws Exception {
        long alphaWorkOrderId = createManualWorkOrder("alpha crack", "Changsha alpha section", SeverityLevel.HIGH);
        assignWorkOrder(alphaWorkOrderId, "zhangsan");

        createManualWorkOrder("beta crack", "Changsha beta section", SeverityLevel.LOW);

        MvcResult result = mockMvc.perform(get("/api/work-orders")
                        .param("status", WorkOrderStatus.ASSIGNED.name())
                        .param("keyword", "alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode records = readBody(result).path("data").path("records");
        assertEquals(1, records.size());
        assertEquals(alphaWorkOrderId, records.get(0).path("id").asLong());
        assertEquals(WorkOrderStatus.ASSIGNED.name(), records.get(0).path("status").asText());
    }

    @Test
    void cancelWorkOrderShouldSucceedForAssignedWorkOrder() throws Exception {
        long workOrderId = createManualWorkOrder("cancel target", "Changsha cancel section", SeverityLevel.MEDIUM);
        assignWorkOrder(workOrderId, "lisi");

        CancelWorkOrderRequest request = new CancelWorkOrderRequest("unable to enter construction area");
        mockMvc.perform(put("/api/work-orders/{workOrderId}/cancel", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(WorkOrderStatus.CANCELLED.name()))
                .andExpect(jsonPath("$.data.statusLogs.length()").value(2));
    }

    @Test
    void deleteDetectionTaskShouldRejectCompletedTask() throws Exception {
        long taskId = createDetectionTask("delete-check.jpg", "Changsha delete section");

        mockMvc.perform(post("/api/detection-tasks/{taskId}/execute", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        waitForDetectionResult(taskId);

        mockMvc.perform(delete("/api/detection-tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void listMaintenanceReportsShouldFilterByExecutor() throws Exception {
        long firstWorkOrderId = createManualWorkOrder("report alpha", "Changsha report alpha", SeverityLevel.HIGH);
        completeWorkOrder(firstWorkOrderId, "zhangsan");
        createMaintenanceReport(firstWorkOrderId, "zhangsan");

        long secondWorkOrderId = createManualWorkOrder("report beta", "Changsha report beta", SeverityLevel.MEDIUM);
        completeWorkOrder(secondWorkOrderId, "lisi");
        createMaintenanceReport(secondWorkOrderId, "lisi");

        MvcResult result = mockMvc.perform(get("/api/maintenance-reports")
                        .param("executor", "zhang"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode records = readBody(result).path("data").path("records");
        assertEquals(1, records.size());
        assertEquals(firstWorkOrderId, records.get(0).path("workOrderId").asLong());
        assertEquals("zhangsan", records.get(0).path("executor").asText());
    }

    private long createDetectionTask(String fileName, String location) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/detection-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.roadcrack.api.request.detection.CreateDetectionTaskRequest(
                                DataSourceType.MANUAL_IMAGE,
                                fileName,
                                "http://example.com/" + fileName,
                                location,
                                "workflow query test"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        return readBody(result).path("data").path("id").asLong();
    }

    private long createManualWorkOrder(String title, String location, SeverityLevel severityLevel) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkOrderRequest(
                                System.nanoTime(),
                                title,
                                DamageType.CRACK,
                                severityLevel,
                                location,
                                DepartmentCode.ROAD_ADMIN,
                                "http://example.com/" + title.replace(' ', '-') + ".jpg",
                                "workflow query test work order"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        return readBody(result).path("data").path("id").asLong();
    }

    private void assignWorkOrder(long workOrderId, String assignee) throws Exception {
        mockMvc.perform(put("/api/work-orders/{workOrderId}/assign", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignWorkOrderRequest(DepartmentCode.ROAD_ADMIN, assignee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(WorkOrderStatus.ASSIGNED.name()));
    }

    private void completeWorkOrder(long workOrderId, String assignee) throws Exception {
        assignWorkOrder(workOrderId, assignee);
        updateWorkOrderStatus(workOrderId, WorkOrderStatus.IN_PROGRESS, "arrived on site");
        updateWorkOrderStatus(workOrderId, WorkOrderStatus.COMPLETED, "repair completed");
    }

    private void updateWorkOrderStatus(long workOrderId, WorkOrderStatus statusValue, String note) throws Exception {
        mockMvc.perform(put("/api/work-orders/{workOrderId}/status", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateWorkOrderStatusRequest(statusValue, note))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(statusValue.name()));
    }

    private void createMaintenanceReport(long workOrderId, String executor) throws Exception {
        mockMvc.perform(post("/api/maintenance-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMaintenanceReportRequest(
                                workOrderId,
                                executor,
                                "http://example.com/before-" + workOrderId + ".jpg",
                                "http://example.com/after-" + workOrderId + ".jpg",
                                "asphalt",
                                "report created in workflow query test",
                                LocalDateTime.now()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private JsonNode waitForDetectionResult(long taskId) throws Exception {
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
        assertTrue(!latestBody.path("data").isMissingNode() && !latestBody.path("data").isNull());
        return latestBody.path("data");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
