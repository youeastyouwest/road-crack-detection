package com.roadcrack.api.response.statistics;

import java.util.List;

public record DepartmentWorkloadResponse(
    List<DeptItem> items
) {
    public record DeptItem(String deptName, int total, int completed, int pending) {}

    public static DepartmentWorkloadResponse empty() {
        return new DepartmentWorkloadResponse(List.of());
    }
}