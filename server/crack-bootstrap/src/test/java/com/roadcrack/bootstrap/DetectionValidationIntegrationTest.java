package com.roadcrack.bootstrap;

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
class DetectionValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createDetectionTaskShouldRejectBlankFileUrl() throws Exception {
        CreateDetectionTaskRequest request = new CreateDetectionTaskRequest(
                DataSourceType.MANUAL_IMAGE,
                "road-crack-01.jpg",
                "",
                "Changsha test road",
                "test task"
        );

        mockMvc.perform(post("/api/detection-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void detectionResultShouldReturnConflictBeforeExecution() throws Exception {
        Long taskId = createDetectionTask();

        mockMvc.perform(get("/api/detection-tasks/{taskId}/result", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void assignWorkOrderShouldRejectBlankAssignee() throws Exception {
        long workOrderId = createManualWorkOrder();
        AssignWorkOrderRequest request = new AssignWorkOrderRequest(DepartmentCode.ROAD_ADMIN, "");

        mockMvc.perform(put("/api/work-orders/{workOrderId}/assign", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void workOrderShouldRejectSkippingAssignment() throws Exception {
        long workOrderId = createManualWorkOrder();
        UpdateWorkOrderStatusRequest request = new UpdateWorkOrderStatusRequest(WorkOrderStatus.IN_PROGRESS, "skip assign");

        mockMvc.perform(put("/api/work-orders/{workOrderId}/status", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void maintenanceReportShouldRejectMissingFinishedAt() throws Exception {
        CreateMaintenanceReportRequest request = new CreateMaintenanceReportRequest(
                1L,
                "lisi",
                null,
                null,
                null,
                "missing finishedAt",
                null
        );

        mockMvc.perform(post("/api/maintenance-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private Long createDetectionTask() throws Exception {
        CreateDetectionTaskRequest request = new CreateDetectionTaskRequest(
                DataSourceType.MANUAL_IMAGE,
                "road-crack-02.jpg",
                "http://example.com/road-crack-02.jpg",
                "Changsha validation road",
                "validation task"
        );

        MvcResult result = mockMvc.perform(post("/api/detection-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private long createManualWorkOrder() throws Exception {
        CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                1000L,
                "manual-validation-work-order",
                DamageType.CRACK,
                SeverityLevel.MEDIUM,
                "Changsha validation section",
                DepartmentCode.ROAD_ADMIN,
                "http://example.com/evidence-validation.jpg",
                "validation work order"
        );

        MvcResult result = mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
