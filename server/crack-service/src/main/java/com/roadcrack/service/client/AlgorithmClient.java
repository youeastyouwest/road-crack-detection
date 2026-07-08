package com.roadcrack.service.client;

import com.roadcrack.service.model.DetectionAnalysisResult;
import com.roadcrack.service.model.DetectionTaskAggregate;

public interface AlgorithmClient {

    DetectionAnalysisResult analyze(DetectionTaskAggregate task);
}
