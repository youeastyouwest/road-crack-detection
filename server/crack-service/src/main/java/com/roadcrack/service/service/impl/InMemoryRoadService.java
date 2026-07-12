package com.roadcrack.service.service.impl;

import com.roadcrack.api.enums.DetectionTaskStatus;
import com.roadcrack.api.response.detection.DetectionItemResponse;
import com.roadcrack.api.response.detection.DetectionTaskResponse;
import com.roadcrack.api.response.road.RoadDiseaseSummaryResponse;
import com.roadcrack.api.response.road.RoadDiseaseSummaryResponse.DiseasePoint;
import com.roadcrack.api.response.road.RoadResponse;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.DetectionTaskService;
import com.roadcrack.service.service.RoadService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryRoadService implements RoadService {

    private final DetectionTaskService detectionTaskService;
    private final Map<Long, SeededRoad> roads = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public InMemoryRoadService(DetectionTaskService detectionTaskService) {
        this.detectionTaskService = detectionTaskService;
        seedRoads();
    }

    @Override
    public PageResponse<RoadResponse> page(int page, int size, String roadName, String district, String roadGrade, String status) {
        List<RoadResponse> all = listAll();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        long total = all.size();
        long pages = total == 0 ? 0 : (total + safeSize - 1) / safeSize;
        return new PageResponse<>(all.subList(from, to), total, safeSize, safePage, pages);
    }

    @Override
    public List<RoadResponse> listAll() {
        List<RoadResponse> result = new ArrayList<>();
        for (SeededRoad r : roads.values()) {
            result.add(r.toResponse());
        }
        return result;
    }

    @Override
    public List<RoadDiseaseSummaryResponse> getRoadsWithDisease() {
        // 1. Get all completed tasks with location data
        PageResponse<DetectionTaskResponse> page = detectionTaskService.listTasks(1, Integer.MAX_VALUE, DetectionTaskStatus.COMPLETED, null, null, null);
        List<DetectionTaskResponse> completedTasks = page.records();

        // 2. Group completed tasks by matched road (优先使用 task.roadId，其次坐标匹配)
        Map<Long, List<DetectionTaskResponse>> roadTaskMap = new HashMap<>();
        for (DetectionTaskResponse task : completedTasks) {
            if (task.location() == null || task.location().isBlank()) continue;

            Long roadId = null;
            // 优先使用 task.roadId 直接匹配
            if (task.roadId() != null && roads.containsKey(task.roadId())) {
                roadId = task.roadId();
            } else {
                // 其次按坐标匹配
                double[] coords = parseLocation(task.location());
                if (coords != null) {
                    SeededRoad matched = matchRoad(coords[0], coords[1]);
                    if (matched != null) {
                        roadId = matched.id;
                    }
                }
            }

            if (roadId != null) {
                roadTaskMap.computeIfAbsent(roadId, k -> new ArrayList<>()).add(task);
            }
        }

        // 3. Build response per road
        List<RoadDiseaseSummaryResponse> result = new ArrayList<>();
        for (Map.Entry<Long, List<DetectionTaskResponse>> entry : roadTaskMap.entrySet()) {
            SeededRoad road = roads.get(entry.getKey());
            if (road == null) continue;

            List<DiseasePoint> points = new ArrayList<>();
            int highCount = 0, mediumCount = 0, lowCount = 0;

            for (DetectionTaskResponse task : entry.getValue()) {
                if (task.result() == null || task.result().items() == null) continue;
                double[] coords = parseLocation(task.location());

                for (DetectionItemResponse item : task.result().items()) {
                    int severityScore = severityScore(item.severityLevel());
                    if (severityScore >= 3) highCount++;
                    else if (severityScore == 2) mediumCount++;
                    else lowCount++;

                    DiseasePoint dp = new DiseasePoint(
                            task.id(),
                            coords != null ? coords[0] : 0,
                            coords != null ? coords[1] : 0,
                            item.damageType() != null ? item.damageType().name() : "UNKNOWN",
                            item.severityLevel() != null ? item.severityLevel().name() : "UNKNOWN",
                            item.damageType() != null ? item.confidence() : 0,
                            task.createdAt() != null ? task.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : ""
                    );
                    // 传入 AI 识别结果图和原始上传图
                    dp.setImageBase64(task.result().imageBase64() != null ? task.result().imageBase64() : "");
                    dp.setFileUrl(task.fileUrl() != null ? task.fileUrl() : "");
                    // 传入病害区域尺寸
                    if (item.boundingBox() != null) {
                        dp.setBbox(item.boundingBox().width() + "x" + item.boundingBox().height() + " px");
                    }
                    points.add(dp);
                }
            }

            String compositeSeverity = highCount > 0 ? "HIGH" : mediumCount > 0 ? "MEDIUM" : "LOW";
            int totalCount = highCount + mediumCount + lowCount;

            result.add(new RoadDiseaseSummaryResponse(
                    road.id, road.name, road.centerLng, road.centerLat,
                    road.path, compositeSeverity, totalCount,
                    highCount, mediumCount, lowCount, points
            ));
        }



        return result;
    }

            @Override
    public List<RoadResponse> listRoadsWithDetections() {
        // 内存模式下返回所有有检测数据的道路
        return listAll();
    }

    private void seedRoads() {
        // Beijing area seeded roads for demonstration
        addRoad("ChangAn Street", 39.909, 116.397, new double[][]{{116.38,39.908},{116.42,39.910}});
        addRoad("2nd Ring Road", 39.91, 116.39, new double[][]{{116.37,39.90},{116.37,39.92},{116.41,39.92},{116.41,39.90}});
        addRoad("3rd Ring Road", 39.91, 116.40, new double[][]{{116.38,39.89},{116.38,39.93},{116.42,39.93},{116.42,39.89}});
        addRoad("4th Ring Road", 39.95, 116.43, new double[][]{{116.42,39.91},{116.44,39.98}});
        addRoad("5th Ring Road", 39.95, 116.35, new double[][]{{116.35,39.88},{116.35,39.98}});
        addRoad("Airport Expressway", 39.905, 116.42, new double[][]{{116.40,39.906},{116.44,39.904}});
        addRoad("Jingzang Expwy", 39.96, 116.35, new double[][]{{116.35,39.94},{116.35,39.98}});
        addRoad("Xizhimen Outer St", 39.94, 116.35, new double[][]{{116.33,39.94},{116.37,39.94}});
    }

    private void addRoad(String name, double centerLat, double centerLng, double[][] pathPoints) {
        long id = idGen.getAndIncrement();
        List<double[]> pathList = new ArrayList<>();
        for (double[] p : pathPoints) {
            pathList.add(new double[]{p[0], p[1]});
        }
        roads.put(id, new SeededRoad(id, name, centerLng, centerLat, pathList));
    }

    private SeededRoad matchRoad(double lng, double lat) {
        SeededRoad best = null;
        double bestDist = Double.MAX_VALUE;
        for (SeededRoad road : roads.values()) {
            double dist = haversineToPolyline(lng, lat, road.path);
            if (dist < bestDist) {
                bestDist = dist;
                best = road;
            }
        }
        // Match if within 2km of nearest seeded road
        return bestDist < 2.0 ? best : null;
    }

    private double haversineToPolyline(double lng, double lat, List<double[]> polyline) {
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < polyline.size() - 1; i++) {
            double[] a = polyline.get(i);
            double[] b = polyline.get(i + 1);
            double dist = pointToSegmentDistanceKm(lng, lat, a[0], a[1], b[0], b[1]);
            if (dist < minDist) minDist = dist;
        }
        return minDist;
    }

    private double pointToSegmentDistanceKm(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0) return haversineKm(px, py, ax, ay);
        double t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / lenSq));
        double cx = ax + t * dx, cy = ay + t * dy;
        return haversineKm(px, py, cx, cy);
    }

    private double haversineKm(double lng1, double lat1, double lng2, double lat2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double[] parseLocation(String location) {
        if (location == null || location.isBlank()) return null;
        try {
            String trimmed = location.trim();
            if (trimmed.contains(",")) {
                String[] parts = trimmed.split(",");
                return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
            } else if (trimmed.matches("\\d+\\.?\\d*\\s+\\d+\\.?\\d*")) {
                String[] parts = trimmed.split("\\s+");
                return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
            }
        } catch (Exception ignored) {}
        return null;
    }

    private int severityScore(com.roadcrack.api.enums.SeverityLevel level) {
        if (level == null) return 0;
        return switch (level) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    private static class SeededRoad {
        final long id;
        final String name;
        final double centerLng;
        final double centerLat;
        final List<double[]> path;

        SeededRoad(long id, String name, double centerLng, double centerLat, List<double[]> path) {
            this.id = id;
            this.name = name;
            this.centerLng = centerLng;
            this.centerLat = centerLat;
            this.path = path;
        }

        RoadResponse toResponse() {
            RoadResponse r = new RoadResponse();
            r.setId(id);
            r.setRoadCode("RD-" + String.format("%04d", id));
            r.setRoadName(name);
            r.setRoadGrade("Arterial Road");
            r.setDistrict("Beijing");
            r.setStartPoint(centerLat + "," + centerLng);
            r.setEndPoint(centerLat + "," + centerLng);
            r.setLengthKm(new BigDecimal("2.5"));
            r.setLaneCount(6);
            r.setSurfaceType("Asphalt");
            r.setBuiltYear(2010);
            r.setLastMaintained(LocalDateTime.now().minusMonths(3));
            r.setHealthScore(new BigDecimal("85.0"));
            r.setDamageLevel("Good");
            r.setLatestDetectionAt(LocalDateTime.now());
            r.setTotalDetectionCount(0);
            r.setCurrentDamageCount(0);
            r.setDepartmentCode("DEPT_BJ");
            r.setStatus("NORMAL");
            r.setRemark("");
            r.setCreatedAt(LocalDateTime.now().minusMonths(6));
            return r;
        }
    }
}