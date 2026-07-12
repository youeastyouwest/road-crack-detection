package com.roadcrack.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 高德地图 Web API 逆地理编码客户端。
 *
 * 调用高德逆地理编码 API，根据经纬度反查真实道路名称。
 * API 文档：https://lbs.amap.com/api/webservice/guide/api/georegeo
 *
 * 优先级提取道路名：roads[0].name > addressComponent.street > 正则匹配 formattedAddress
 */
@Component
public class AmapGeocodeClient {

    private static final Logger log = LoggerFactory.getLogger(AmapGeocodeClient.class);
    private static final String API_URL = "https://restapi.amap.com/v3/geocode/regeo";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${amap.api-key:}")
    private String apiKey;

    /**
     * 根据经纬度反查道路名称。
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 道路名称，如果解析失败或无 API Key 则返回空字符串
     */
    public String reverseGeocode(double lng, double lat) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("高德 API Key 未配置，跳过逆地理编码");
            return "";
        }

        try {
            String url = API_URL + "?key=" + apiKey
                    + "&location=" + lng + "," + lat
                    + "&extensions=base"
                    + "&radius=1000"
                    + "&output=JSON";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("高德逆地理编码请求失败，状态码: {}", response.statusCode());
                return "";
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());

            // 检查高德返回状态
            String status = root.path("status").asText();
            if (!"1".equals(status)) {
                log.debug("高德逆地理编码返回非成功状态: {}，info: {}",
                        status, root.path("info").asText(""));
                return "";
            }

            JsonNode regeocode = root.path("regeocode");
            if (regeocode.isMissingNode()) {
                return "";
            }

            // 优先级 1：roads 数组中的道路名（最精确）
            JsonNode roads = regeocode.path("roads");
            if (roads.isArray() && roads.size() > 0) {
                JsonNode firstRoad = roads.get(0);
                String roadName = firstRoad.path("name").asText(null);
                if (roadName != null && !roadName.isBlank()) {
                    return roadName.trim();
                }
            }

            // 优先级 2：addressComponent.street
            JsonNode addressComponent = regeocode.path("addressComponent");
            if (!addressComponent.isMissingNode()) {
                JsonNode streetNode = addressComponent.path("street");
                if (!streetNode.isMissingNode()) {
                    String street = streetNode.asText(null);
                    if (street != null && !street.isBlank() && !"[]".equals(street)) {
                        return street.trim();
                    }
                }
            }

            // 优先级 3：从 formattedAddress 正则提取道路名
            String formattedAddress = regeocode.path("formattedAddress").asText(null);
            if (formattedAddress != null && !formattedAddress.isBlank()) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "[\\u4e00-\\u9fa5]+(?:路|街|道|巷|胡同|桥|高速|环路|大街|大道|快速路|公路|辅路)"
                ).matcher(formattedAddress);
                if (m.find()) {
                    return m.group().trim();
                }
            }

            return "";
        } catch (Exception e) {
            log.warn("高德逆地理编码异常: lng={}, lat={}, error={}", lng, lat, e.getMessage());
            return "";
        }
    }

    /**
     * 高德 API Key 是否已配置
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
