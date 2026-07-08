package com.roadcrack.service.client;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.response.detection.BoundingBoxResponse;
import com.roadcrack.api.response.detection.DetectionItemResponse;
import com.roadcrack.service.model.DetectionAnalysisResult;
import com.roadcrack.service.model.DetectionTaskAggregate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockAlgorithmClient implements AlgorithmClient {

    @Override
    public DetectionAnalysisResult analyze(DetectionTaskAggregate task) {
        int seed = Math.abs((task.getFileName() + "|" + task.getLocation()).hashCode());
        DamageType primaryType = resolveDamageType(task.getFileName(), seed);
        SeverityLevel severityLevel = resolveSeverity(seed);

        List<DetectionItemResponse> items = new ArrayList<>();
        items.add(new DetectionItemResponse(
                primaryType,
                severityLevel,
                0.78 + (seed % 15) / 100.0,
                new BoundingBoxResponse(60 + seed % 80, 40 + seed % 70, 120 + seed % 50, 36 + seed % 25),
                resolveSuggestion(primaryType, severityLevel)
        ));

        if (seed % 3 == 0) {
            items.add(new DetectionItemResponse(
                    DamageType.CRACK,
                    SeverityLevel.LOW,
                    0.72,
                    new BoundingBoxResponse(180, 120, 96, 20),
                    "建议纳入定期巡检，观察裂缝是否继续扩展。"
            ));
        }

        String summary = "已完成占位检测，共识别 " + items.size() + " 处疑似病害，建议按严重程度进入后续工单流转。";
        return new DetectionAnalysisResult(summary, items);
    }

    private DamageType resolveDamageType(String fileName, int seed) {
        String lowered = fileName.toLowerCase();
        if (lowered.contains("spill")) {
            return DamageType.ROAD_SPILL;
        }
        if (lowered.contains("line")) {
            return DamageType.MARKING_DAMAGE;
        }
        if (lowered.contains("pothole")) {
            return DamageType.POTHOLE;
        }

        DamageType[] candidates = {DamageType.CRACK, DamageType.MARKING_DAMAGE, DamageType.ROAD_SPILL, DamageType.POTHOLE};
        return candidates[seed % candidates.length];
    }

    private SeverityLevel resolveSeverity(int seed) {
        SeverityLevel[] levels = {SeverityLevel.LOW, SeverityLevel.MEDIUM, SeverityLevel.HIGH};
        return levels[seed % levels.length];
    }

    private String resolveSuggestion(DamageType damageType, SeverityLevel severityLevel) {
        if (severityLevel == SeverityLevel.HIGH) {
            return "建议立即派单，优先安排现场核查与处置。";
        }
        return switch (damageType) {
            case ROAD_SPILL -> "建议通知环卫部门尽快清理，避免二次交通风险。";
            case MARKING_DAMAGE -> "建议路政部门补画或修复标志线。";
            case POTHOLE -> "建议安排坑槽修补，必要时同步交通管制。";
            case CRACK, UNKNOWN -> "建议继续复核裂缝宽度和延展趋势。";
        };
    }
}
