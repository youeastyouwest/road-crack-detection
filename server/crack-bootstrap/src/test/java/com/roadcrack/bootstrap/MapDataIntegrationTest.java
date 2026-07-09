package com.roadcrack.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadcrack.api.enums.DataSourceType;
import com.roadcrack.api.request.detection.CreateDetectionTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "crack.persistence.mode=memory",
        "crack.algorithm.mock-enabled=true"
})
class MapDataIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void mapApisShouldReturnMarkersStatisticsTrendAndRatios() throws Exception {
        long crackTaskId = createAndExecuteTask("road-crack-map-01.jpg", "Changsha Yuelu Avenue");
        createAndExecuteTask("road-spill-map-02.jpg", "Changsha Furong Road");

        MvcResult markerResult = mockMvc.perform(get("/api/map/markers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data[0].hasCoordinates").value(true))
                .andReturn();

        JsonNode markerData = readBody(markerResult).path("data");
        long markerId = markerData.get(0).path("id").asLong();

        mockMvc.perform(get("/api/map/markers/{markerId}", markerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(markerId))
                .andExpect(jsonPath("$.data.taskId").isNumber())
                .andExpect(jsonPath("$.data.damageType").isString());

        MvcResult statisticsResult = mockMvc.perform(get("/api/map/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalDamageCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.workOrderCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andReturn();

        JsonNode statistics = readBody(statisticsResult).path("data");
        assertTrue(statistics.path("mappedPointCount").asLong() >= 2);

        mockMvc.perform(get("/api/map/trend").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(7));

        MvcResult ratioResult = mockMvc.perform(get("/api/map/damage-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andReturn();

        long ratioCountSum = 0L;
        for (JsonNode item : readBody(ratioResult).path("data")) {
            ratioCountSum += item.path("count").asLong();
        }
        assertEquals(statistics.path("totalDamageCount").asLong(), ratioCountSum);

        mockMvc.perform(get("/api/map/markers").param("keyword", "Yuelu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].taskId").value(crackTaskId));
    }

    private long createAndExecuteTask(String fileName, String location) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/detection-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDetectionTaskRequest(
                                DataSourceType.MANUAL_IMAGE,
                                fileName,
                                "http://example.com/" + fileName,
                                location,
                                "map api test"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        long taskId = readBody(createResult).path("data").path("id").asLong();

        mockMvc.perform(post("/api/detection-tasks/{taskId}/execute", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        waitForDetectionResult(taskId);
        return taskId;
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
